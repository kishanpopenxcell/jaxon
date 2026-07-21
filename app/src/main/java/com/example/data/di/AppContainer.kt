package com.example.data.di

import android.content.Context
import com.example.data.database.JaxonDatabase
import com.example.data.repository.JaxonRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.executor.ActionExecutor
import com.example.domain.parser.IntentParser
import com.example.domain.speech.SpeechManager
import com.example.domain.tts.TextToSpeechManager

class AppContainer(private val context: Context) {

    val database: JaxonDatabase by lazy {
        JaxonDatabase.getDatabase(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val jaxonRepository: JaxonRepository by lazy {
        JaxonRepository(
            voiceCommandDao = database.voiceCommandDao(),
            customRoutineDao = database.customRoutineDao(),
            settings = settingsRepository
        )
    }

    val speechManager: SpeechManager by lazy {
        SpeechManager(context)
    }

    val ttsManager: TextToSpeechManager by lazy {
        TextToSpeechManager(context)
    }

    val intentParser: IntentParser by lazy {
        IntentParser()
    }

    val actionExecutor: ActionExecutor by lazy {
        ActionExecutor(context)
    }
}
