package com.example.domain.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechState {
    IDLE,
    READY,
    LISTENING,
    PROCESSING,
    SUCCESS,
    ERROR
}

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow(SpeechState.IDLE)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0.0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.READY
            _errorMessage.value = null
        }

        override fun onBeginningOfSpeech() {
            _state.value = SpeechState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Keep RMS within a clean normalized bounds
            _rmsDb.value = rmsdB.coerceIn(0.0f, 10.0f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = SpeechState.PROCESSING
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client-side connection error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Assistant is busy. Try again."
                SpeechRecognizer.ERROR_SERVER -> "Server-side error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence detected. Please try again."
                else -> "Speech recognition failed"
            }
            _state.value = SpeechState.ERROR
            _errorMessage.value = message
            _rmsDb.value = 0.0f
        }

        override fun onResults(results: Bundle?) {
            _state.value = SpeechState.SUCCESS
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                _finalText.value = bestMatch
                _partialText.value = bestMatch
            } else {
                _state.value = SpeechState.ERROR
                _errorMessage.value = "No voice matches found"
            }
            _rmsDb.value = 0.0f
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _partialText.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        } else {
            _state.value = SpeechState.ERROR
            _errorMessage.value = "Speech recognition is not available on this device"
        }
    }

    fun startListening(locale: String = "en-US") {
        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        _partialText.value = ""
        _finalText.value = ""
        _errorMessage.value = null
        _rmsDb.value = 0.0f

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _state.value = SpeechState.READY
        } catch (e: Exception) {
            _state.value = SpeechState.ERROR
            _errorMessage.value = "Failed to start listening: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _state.value = SpeechState.IDLE
            _rmsDb.value = 0.0f
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Safe ignore
        }
    }
}
