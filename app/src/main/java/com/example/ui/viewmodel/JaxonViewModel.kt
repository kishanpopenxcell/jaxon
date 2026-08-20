package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomRoutine
import com.example.data.model.VoiceCommand
import com.example.data.repository.JaxonRepository
import com.example.domain.executor.ActionExecutor
import com.example.domain.parser.IntentParser
import com.example.domain.parser.IntentType
import com.example.domain.parser.ParsedIntent
import com.example.domain.service.JaxonBackgroundService
import com.example.domain.service.WakeEvent
import com.example.domain.speech.MicOwner
import com.example.domain.speech.MicOwnership
import com.example.domain.speech.SpeechManager
import com.example.domain.speech.SpeechState
import com.example.domain.tts.TextToSpeechManager
import com.example.ui.components.JaxonFaceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JaxonViewModel(
    private val app: Application,
    private val repository: JaxonRepository,
    val speechManager: SpeechManager,
    private val ttsManager: TextToSpeechManager,
    private val intentParser: IntentParser,
    private val actionExecutor: ActionExecutor
) : AndroidViewModel(app) {

    // Speech Flows observed directly
    val speechState: StateFlow<SpeechState> = speechManager.state
    val partialText: StateFlow<String> = speechManager.partialText
    val finalText: SharedFlow<String> = speechManager.finalText
    val rmsDb: StateFlow<Float> = speechManager.rmsDb
    val speechError: StateFlow<String?> = speechManager.errorMessage

    // Local DB Flows observed from repository
    val historyState: StateFlow<List<VoiceCommand>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesState: StateFlow<List<VoiceCommand>> = repository.favoriteCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routinesState: StateFlow<List<CustomRoutine>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Datastore Preferences Flows
    val isOnboardingCompleted = repository.settings.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTtsEnabled = repository.settings.isTtsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val ttsRate = repository.settings.ttsRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val ttsPitch = repository.settings.ttsPitch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val isBackgroundServiceEnabled = repository.settings.isBackgroundServiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recognitionLocale = repository.settings.recognitionLocale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en-US")

    // Active listening UI Overlay state
    private val _showListeningOverlay = MutableStateFlow(false)
    val showListeningOverlay: StateFlow<Boolean> = _showListeningOverlay.asStateFlow()

    private val _lastParsedIntent = MutableStateFlow<ParsedIntent?>(null)
    val lastParsedIntent: StateFlow<ParsedIntent?> = _lastParsedIntent.asStateFlow()

    private val _activeResultText = MutableStateFlow<String?>(null)
    val activeResultText: StateFlow<String?> = _activeResultText.asStateFlow()

    private val _isExecutingAction = MutableStateFlow(false)
    val isExecutingAction: StateFlow<Boolean> = _isExecutingAction.asStateFlow()

    // Transient Success/Error pulse shown briefly after an action completes, then cleared
    // back to null so faceState falls through to the live speech/execution state again.
    private val _completionPulse = MutableStateFlow<JaxonFaceState?>(null)

    val faceState: StateFlow<JaxonFaceState> = combine(
        speechState, isExecutingAction, _completionPulse
    ) { speech, executing, pulse ->
        when {
            pulse != null -> pulse
            executing -> JaxonFaceState.PROCESSING
            speech == SpeechState.LISTENING || speech == SpeechState.READY -> JaxonFaceState.LISTENING
            speech == SpeechState.PROCESSING -> JaxonFaceState.PROCESSING
            else -> JaxonFaceState.IDLE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JaxonFaceState.IDLE)

    init {
        // React to wake-word events from the background service: either a plain "start
        // listening" request (from the notification), or a detected "Hey Jaxon" - optionally
        // already carrying the trailing command the user spoke in the same breath.
        viewModelScope.launch {
            JaxonBackgroundService.wakeEvents.collect { event ->
                when (event) {
                    is WakeEvent.StartListening -> startListening()
                    is WakeEvent.WakeDetected -> {
                        val command = event.trailingCommand
                        if (!command.isNullOrBlank()) {
                            executeVoiceText(command)
                        } else {
                            startListening()
                        }
                    }
                }
            }
        }

        // Collect speech results automatically to invoke parsing and execution. Uses plain
        // collect (not collectLatest) so a slow-running command/routine execution is never
        // cancelled by a later, unrelated emission.
        viewModelScope.launch {
            finalText.collect { text ->
                if (text.isNotEmpty()) {
                    executeVoiceText(text)
                }
            }
        }
    }

    fun startListening() {
        _showListeningOverlay.value = true
        _lastParsedIntent.value = null
        _activeResultText.value = null
        viewModelScope.launch {
            // Make sure the background wake-word loop has released the mic before the UI
            // claims it, avoiding a SpeechRecognizer contention error.
            JaxonBackgroundService.requestPauseWakeLoop(app)
            MicOwnership.forceClaim(MicOwner.UI)
            delay(300)
            speechManager.startListening(recognitionLocale.value)
        }
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun cancelListening() {
        speechManager.cancel()
        _showListeningOverlay.value = false
        releaseMicAndResumeWakeLoop()
    }

    fun closeListeningOverlay() {
        _showListeningOverlay.value = false
        releaseMicAndResumeWakeLoop()
    }

    private fun releaseMicAndResumeWakeLoop() {
        Log.d("JaxonViewModel", "releaseMicAndResumeWakeLoop called")
        MicOwnership.release(MicOwner.UI)
        JaxonBackgroundService.notifyWakeCommandFinished(app)
    }

    /**
     * Parse, check for local automations/routines, execute, speak outcome, and store history.
     */
    fun executeVoiceText(text: String) {
        if (text.isBlank()) return
        _showListeningOverlay.value = true
        _isExecutingAction.value = true

        viewModelScope.launch {
            try {
                val normalizedText = text.lowercase().trim()

                // 1. Check if it matches an active user-defined Custom Routine
                val matchedRoutine = repository.getEnabledRoutineByPhrase(normalizedText)
                if (matchedRoutine != null) {
                    // Execute Custom Multi-step routine
                    val actions = matchedRoutine.actionsJson.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (actions.isEmpty()) {
                        completeExecution(
                            intentName = "CUSTOM_ROUTINE",
                            confidence = 1.0f,
                            originalText = text,
                            normalizedText = normalizedText,
                            resultText = "Custom routine '${matchedRoutine.triggerPhrase}' is empty."
                        )
                        return@launch
                    }

                    val resultsList = mutableListOf<String>()
                    resultsList.add("Running routine '${matchedRoutine.triggerPhrase}'")

                    for ((idx, actionText) in actions.withIndex()) {
                        delay(800) // slight buffer between actions for visual tracking and scheduling
                        val subParsed = intentParser.parse(actionText)
                        val result = actionExecutor.execute(subParsed)
                        resultsList.add("${idx + 1}: $result")
                    }

                    val finalOutcome = resultsList.joinToString("\n")
                    val speakOutcome = "Triggering routine ${matchedRoutine.triggerPhrase}. Completed ${actions.size} actions successfully."

                    completeExecution(
                        intentName = "CUSTOM_ROUTINE",
                        confidence = 1.0f,
                        originalText = text,
                        normalizedText = normalizedText,
                        resultText = finalOutcome,
                        speakText = speakOutcome
                    )
                } else {
                    // 2. Normal intent parsing
                    val parsed = intentParser.parse(text)
                    _lastParsedIntent.value = parsed

                    val result = actionExecutor.execute(parsed)

                    completeExecution(
                        intentName = parsed.intentType.name,
                        confidence = parsed.confidence,
                        originalText = text,
                        normalizedText = parsed.normalizedText,
                        resultText = result
                    )
                }
            } finally {
                // Guarantees the mic is always handed back to the wake-word loop, even if
                // parsing/execution throws - otherwise a single bad command could permanently
                // strand the mic with MicOwner.UI and silently kill "Hey Jaxon" forever.
                _isExecutingAction.value = false
                releaseMicAndResumeWakeLoop()
            }
        }
    }

    private suspend fun completeExecution(
        intentName: String,
        confidence: Float,
        originalText: String,
        normalizedText: String,
        resultText: String,
        speakText: String = resultText
    ) {
        _activeResultText.value = resultText
        _isExecutingAction.value = false

        // Record to Room DB
        val command = VoiceCommand(
            originalText = originalText,
            normalizedText = normalizedText,
            intentName = intentName,
            confidence = confidence,
            executionResult = resultText
        )
        repository.insertCommand(command)

        // Read TTS settings and speak
        if (isTtsEnabled.value) {
            ttsManager.speak(speakText, ttsRate.value, ttsPitch.value)
        }

        // Brief Success/Error pulse on the face, then fall back to the live speech state.
        _completionPulse.value = if (looksLikeFailure(resultText)) {
            JaxonFaceState.ERROR
        } else {
            JaxonFaceState.SUCCESS
        }
        delay(1200)
        _completionPulse.value = null
    }

    private fun looksLikeFailure(resultText: String): Boolean {
        val failureMarkers = listOf(
            "i need ", "i couldn't", "i am not completely sure", "unable to",
            "an error occurred", "please specify", "please tell me", "how long would you like"
        )
        val lower = resultText.lowercase()
        return failureMarkers.any { lower.startsWith(it) || lower.contains(it) }
    }

    // --- Onboarding Action ---
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.settings.setOnboardingCompleted(true)
        }
    }

    // --- Settings Actions ---
    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.settings.setTtsEnabled(enabled) }
    }

    fun setTtsRate(rate: Float) {
        viewModelScope.launch { repository.settings.setTtsRate(rate) }
    }

    fun setTtsPitch(pitch: Float) {
        viewModelScope.launch { repository.settings.setTtsPitch(pitch) }
    }

    fun setRecognitionLocale(locale: String) {
        viewModelScope.launch { repository.settings.setRecognitionLocale(locale) }
    }

    fun toggleBackgroundService(enabled: Boolean) {
        viewModelScope.launch {
            repository.settings.setBackgroundServiceEnabled(enabled)
            val serviceIntent = Intent(app, JaxonBackgroundService::class.java).apply {
                action = if (enabled) JaxonBackgroundService.ACTION_START_SERVICE else JaxonBackgroundService.ACTION_STOP_SERVICE
            }
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    app.startForegroundService(serviceIntent)
                } else {
                    app.startService(serviceIntent)
                }
            } else {
                // startService (not stopService) so the ACTION_STOP_SERVICE action actually
                // reaches onStartCommand and runs stopEverything() deterministically, instead
                // of relying solely on onDestroy() to clean up the wake-word loop.
                app.startService(serviceIntent)
            }
        }
    }

    // --- History Actions ---
    fun toggleFavorite(command: VoiceCommand) {
        viewModelScope.launch {
            repository.updateCommand(command.copy(isFavorite = !command.isFavorite))
        }
    }

    fun deleteHistoryItem(command: VoiceCommand) {
        viewModelScope.launch {
            repository.deleteCommand(command)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- Custom Routines Actions ---
    fun addRoutine(trigger: String, actions: String) {
        viewModelScope.launch {
            val routine = CustomRoutine(
                triggerPhrase = trigger.lowercase().trim(),
                actionsJson = actions
            )
            repository.insertRoutine(routine)
        }
    }

    fun deleteRoutine(routine: CustomRoutine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    fun toggleRoutineEnabled(routine: CustomRoutine) {
        viewModelScope.launch {
            repository.updateRoutine(routine.copy(isEnabled = !routine.isEnabled))
        }
    }

    fun toggleRoutineFavorite(routine: CustomRoutine) {
        viewModelScope.launch {
            repository.updateRoutine(routine.copy(isFavorite = !routine.isFavorite))
        }
    }

    fun duplicateRoutine(routine: CustomRoutine) {
        viewModelScope.launch {
            val dup = CustomRoutine(
                triggerPhrase = "${routine.triggerPhrase} copy",
                actionsJson = routine.actionsJson,
                isEnabled = routine.isEnabled,
                isFavorite = routine.isFavorite
            )
            repository.insertRoutine(dup)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Deliberately does NOT call ttsManager.shutdown() / speechManager.destroy(): both are
        // Application-scoped singletons owned by AppContainer and may still be needed by the
        // background service (TTS for "Listening, Sir") or a freshly-recreated ViewModel after
        // this one is cleared. Tearing them down here previously left the app's speech/TTS
        // permanently dead until a full process restart.
    }
}

class JaxonViewModelFactory(
    private val app: Application,
    private val repository: JaxonRepository,
    private val speechManager: SpeechManager,
    private val ttsManager: TextToSpeechManager,
    private val intentParser: IntentParser,
    private val actionExecutor: ActionExecutor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JaxonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JaxonViewModel(app, repository, speechManager, ttsManager, intentParser, actionExecutor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
