package com.example.domain.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WakeWordState {
    DISABLED,
    WAKE_LISTENING,
    TRIGGERED,
    PAUSED_BY_UI,
    ERROR_PERMISSION,
    ERROR_UNAVAILABLE
}

/**
 * Owns a dedicated SpeechRecognizer that loops short recognition sessions back-to-back while
 * [start] is active, listening for the "Hey Jaxon" wake phrase. Deliberately separate from
 * [SpeechManager] - which drives the UI's on-demand listening - so a background wake-word
 * session can never contend with (or spam UI state during) a user-triggered command capture.
 * [MicOwnership] is the interlock that guarantees only one of the two is ever recording.
 *
 * All public methods and the recognizer callbacks must run on the main thread (SpeechRecognizer
 * requires a Looper thread); callers should invoke this from Dispatchers.Main.immediate.
 */
class WakeWordEngine(
    private val context: Context,
    private val onWakeDetected: (trailingCommand: String?) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var locale: String = "en-US"
    private var consecutiveClientErrors = 0
    private var restartsThisMinute = 0
    private var minuteWindowStart = 0L

    private val _state = MutableStateFlow(WakeWordState.DISABLED)
    val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val watchdog = Runnable {
        recognizer?.cancel()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            handleHypotheses(matches)
        }

        override fun onResults(results: Bundle?) {
            mainHandler.removeCallbacks(watchdog)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "onResults: $matches")
            if (!handleHypotheses(matches)) {
                MicOwnership.release(MicOwner.WAKE_WORD)
                scheduleRestart(RESTART_DELAY_NORMAL_MS)
            }
        }

        override fun onError(error: Int) {
            mainHandler.removeCallbacks(watchdog)
            MicOwnership.release(MicOwner.WAKE_WORD)
            Log.d(TAG, "onError: $error")

            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _state.value = WakeWordState.ERROR_PERMISSION
                    running = false
                }
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    consecutiveClientErrors = 0
                    scheduleRestart(RESTART_DELAY_NORMAL_MS)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> {
                    consecutiveClientErrors++
                    if (consecutiveClientErrors >= 3) {
                        recreateRecognizer()
                        consecutiveClientErrors = 0
                    }
                    val backoff = (BACKOFF_BASE_MS * (1 shl (consecutiveClientErrors.coerceAtMost(4))))
                        .coerceAtMost(BACKOFF_CAP_MS)
                    scheduleRestart(backoff)
                }
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> {
                    // Same failure-mode family as busy/client: retrying immediately can't help,
                    // so back off the same way instead of a tight fail loop.
                    consecutiveClientErrors++
                    val backoff = (BACKOFF_BASE_MS * (1 shl (consecutiveClientErrors.coerceAtMost(4))))
                        .coerceAtMost(BACKOFF_CAP_MS)
                    scheduleRestart(backoff)
                }
                else -> {
                    consecutiveClientErrors = 0
                    scheduleRestart(BACKOFF_BASE_MS)
                }
            }
        }
    }

    /** Returns the trailing command (if the wake phrase was followed by one) and true if matched. */
    private fun handleHypotheses(matches: List<String>?): Boolean {
        if (matches.isNullOrEmpty()) return false
        val match = WakeWordDetector.findWake(matches) ?: return false

        mainHandler.removeCallbacks(watchdog)
        _state.value = WakeWordState.TRIGGERED
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
            // Safe ignore
        }
        MicOwnership.release(MicOwner.WAKE_WORD)
        onWakeDetected(match.trailingCommand)
        return true
    }

    fun start(locale: String) {
        this.locale = locale
        running = true
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = WakeWordState.ERROR_UNAVAILABLE
            running = false
            return
        }
        armNextSession()
    }

    /** Stops the loop without destroying the recognizer - resumable via [resume]. */
    fun pause() {
        running = false
        mainHandler.removeCallbacks(watchdog)
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
            // Safe ignore
        }
        MicOwnership.release(MicOwner.WAKE_WORD)
        if (_state.value != WakeWordState.ERROR_PERMISSION && _state.value != WakeWordState.ERROR_UNAVAILABLE) {
            _state.value = WakeWordState.PAUSED_BY_UI
        }
    }

    fun resume() {
        if (_state.value == WakeWordState.ERROR_PERMISSION || _state.value == WakeWordState.ERROR_UNAVAILABLE) {
            return
        }
        running = true
        armNextSession()
    }

    /** Fully tears down the recognizer. Idempotent - safe to call multiple times. */
    fun shutdown() {
        running = false
        mainHandler.removeCallbacks(watchdog)
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
            // Safe ignore
        }
        recognizer = null
        MicOwnership.release(MicOwner.WAKE_WORD)
        _state.value = WakeWordState.DISABLED
    }

    private fun recreateRecognizer() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
            // Safe ignore
        }
        recognizer = null
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!running) return
        mainHandler.postDelayed({ armNextSession() }, RESTART_TOKEN, delayMs)
    }

    private fun armNextSession() {
        if (!running) return

        val now = System.currentTimeMillis()
        if (now - minuteWindowStart > 60_000) {
            minuteWindowStart = now
            restartsThisMinute = 0
        }
        restartsThisMinute++
        if (restartsThisMinute > MAX_RESTARTS_PER_MINUTE) {
            // Runaway retry loop guard (e.g. mic held by another app indefinitely) - fall back
            // to a slow poll instead of spinning tightly.
            scheduleRestart(SLOW_POLL_MS)
            return
        }

        if (!MicOwnership.tryClaim(MicOwner.WAKE_WORD)) {
            Log.d(TAG, "armNextSession: mic owned by ${MicOwnership.current()}, retrying shortly")
            // UI currently owns the mic right now. JaxonBackgroundService also calls resume()
            // when the UI releases ownership, but that signal can be missed or race (e.g. if
            // MicOwnership.release(UI) and this check interleave badly) - so this engine must
            // always be able to self-heal by retrying on its own rather than depending solely
            // on an external caller to re-arm it, or a single missed signal kills wake-word
            // detection permanently until the service is restarted.
            scheduleRestart(MIC_BUSY_RETRY_MS)
            return
        }

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Deliberately NOT setting EXTRA_PREFER_OFFLINE: on many devices this combined with
            // a specific EXTRA_LANGUAGE fails instantly with ERROR_LANGUAGE_UNAVAILABLE if no
            // matching offline model is installed, which looped this engine in a fast
            // start/fail cycle (visible as the mic icon flickering) without ever actually
            // listening for speech.
        }

        try {
            _state.value = WakeWordState.WAKE_LISTENING
            recognizer?.startListening(intent)
            Log.d(TAG, "armNextSession: startListening() called")
            mainHandler.postDelayed(watchdog, WATCHDOG_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.d(TAG, "armNextSession: startListening() threw", e)
            MicOwnership.release(MicOwner.WAKE_WORD)
            scheduleRestart(BACKOFF_BASE_MS)
        }
    }

    companion object {
        private const val TAG = "WakeWordEngine"
        private const val RESTART_TOKEN = "wake_word_restart"
        private const val RESTART_DELAY_NORMAL_MS = 300L
        private const val BACKOFF_BASE_MS = 500L
        private const val BACKOFF_CAP_MS = 10_000L
        private const val WATCHDOG_TIMEOUT_MS = 8_000L
        private const val SLOW_POLL_MS = 30_000L
        private const val MAX_RESTARTS_PER_MINUTE = 30
        private const val MIC_BUSY_RETRY_MS = 1_000L
    }
}
