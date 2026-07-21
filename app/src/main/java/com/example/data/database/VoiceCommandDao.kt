package com.example.data.database

import androidx.room.*
import com.example.data.model.VoiceCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM voice_commands ORDER BY timestamp DESC")
    fun getAllCommands(): Flow<List<VoiceCommand>>

    @Query("SELECT * FROM voice_commands WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteCommands(): Flow<List<VoiceCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: VoiceCommand): Long

    @Update
    suspend fun updateCommand(command: VoiceCommand)

    @Delete
    suspend fun deleteCommand(command: VoiceCommand)

    @Query("DELETE FROM voice_commands")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM voice_commands WHERE originalText LIKE :query OR executionResult LIKE :query ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<VoiceCommand>>
}
