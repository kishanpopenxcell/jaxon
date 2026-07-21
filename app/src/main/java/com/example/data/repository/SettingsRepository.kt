package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jaxon_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val KEY_TTS_RATE = floatPreferencesKey("tts_rate")
        private val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        private val KEY_BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        private val KEY_RECOGNITION_LOCALE = stringPreferencesKey("recognition_locale")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isTtsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_ENABLED] ?: true
    }

    val ttsRate: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_RATE] ?: 1.0f
    }

    val ttsPitch: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_PITCH] ?: 1.0f
    }

    val isBackgroundServiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BACKGROUND_SERVICE_ENABLED] ?: false
    }

    val recognitionLocale: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_RECOGNITION_LOCALE] ?: "en-US"
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_ENABLED] = enabled
        }
    }

    suspend fun setTtsRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_RATE] = rate
        }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_PITCH] = pitch
        }
    }

    suspend fun setBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setRecognitionLocale(locale: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RECOGNITION_LOCALE] = locale
        }
    }
}
