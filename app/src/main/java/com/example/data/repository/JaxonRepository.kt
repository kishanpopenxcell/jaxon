package com.example.data.repository

import com.example.data.database.CustomRoutineDao
import com.example.data.database.VoiceCommandDao
import com.example.data.model.CustomRoutine
import com.example.data.model.VoiceCommand
import kotlinx.coroutines.flow.Flow

class JaxonRepository(
    private val voiceCommandDao: VoiceCommandDao,
    private val customRoutineDao: CustomRoutineDao,
    val settings: SettingsRepository
) {
    val allHistory: Flow<List<VoiceCommand>> = voiceCommandDao.getAllCommands()
    val favoriteCommands: Flow<List<VoiceCommand>> = voiceCommandDao.getFavoriteCommands()
    val allRoutines: Flow<List<CustomRoutine>> = customRoutineDao.getAllRoutines()

    suspend fun insertCommand(command: VoiceCommand): Long {
        return voiceCommandDao.insertCommand(command)
    }

    suspend fun updateCommand(command: VoiceCommand) {
        voiceCommandDao.updateCommand(command)
    }

    suspend fun deleteCommand(command: VoiceCommand) {
        voiceCommandDao.deleteCommand(command)
    }

    suspend fun clearHistory() {
        voiceCommandDao.clearAllHistory()
    }

    fun searchHistory(query: String): Flow<List<VoiceCommand>> {
        return voiceCommandDao.searchHistory("%$query%")
    }

    suspend fun insertRoutine(routine: CustomRoutine): Long {
        return customRoutineDao.insertRoutine(routine)
    }

    suspend fun updateRoutine(routine: CustomRoutine) {
        customRoutineDao.updateRoutine(routine)
    }

    suspend fun deleteRoutine(routine: CustomRoutine) {
        customRoutineDao.deleteRoutine(routine)
    }

    suspend fun getEnabledRoutineByPhrase(phrase: String): CustomRoutine? {
        return customRoutineDao.getEnabledRoutineByPhrase(phrase.lowercase().trim())
    }

    suspend fun getRoutineById(id: Int): CustomRoutine? {
        return customRoutineDao.getRoutineById(id)
    }
}
