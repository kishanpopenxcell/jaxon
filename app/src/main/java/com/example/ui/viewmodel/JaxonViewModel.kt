package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
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
import com.example.domain.speech.SpeechManager
import com.example.domain.speech.SpeechState
import com.example.domain.tts.TextToSpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val finalText: StateFlow<String> = speechManager.finalText
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

    init {
        // Collect background service triggers
        viewModelScope.launch {
            JaxonBackgroundService.triggerListenFlow.collectLatest { trigger ->
                if (trigger) {
                    startListening()
                    JaxonBackgroundService.clearTriggerListen()
                }
            }
        }

        // Collect speech success results automatically to invoke parsing and execution
        viewModelScope.launch {
            finalText.collectLatest { text ->
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
            val locale = repository.settings.recognitionLocale.stateIn(viewModelScope).value
            speechManager.startListening(locale)
        }
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun cancelListening() {
        speechManager.cancel()
        _showListeningOverlay.value = false
    }

    fun closeListeningOverlay() {
        _showListeningOverlay.value = false
    }

    /**
     * Parse, check for local automations/routines, execute, speak outcome, and store history.
     */
    fun executeVoiceText(text: String) {
        if (text.isBlank()) return
        _showListeningOverlay.value = true
        _isExecutingAction.value = true

        viewModelScope.launch {
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
        val ttsActive = repository.settings.isTtsEnabled.stateIn(viewModelScope).value
        if (ttsActive) {
            val rate = repository.settings.ttsRate.stateIn(viewModelScope).value
            val pitch = repository.settings.ttsPitch.stateIn(viewModelScope).value
            ttsManager.speak(speakText, rate, pitch)
        }
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
                app.stopService(serviceIntent)
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
        ttsManager.shutdown()
        speechManager.destroy()
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
