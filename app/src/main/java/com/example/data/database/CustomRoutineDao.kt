package com.example.data.database

import androidx.room.*
import com.example.data.model.CustomRoutine
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRoutineDao {
    @Query("SELECT * FROM custom_routines ORDER BY timestamp DESC")
    fun getAllRoutines(): Flow<List<CustomRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: CustomRoutine): Long

    @Update
    suspend fun updateRoutine(routine: CustomRoutine)

    @Delete
    suspend fun deleteRoutine(routine: CustomRoutine)

    @Query("SELECT * FROM custom_routines WHERE triggerPhrase = :phrase AND isEnabled = 1 LIMIT 1")
    suspend fun getEnabledRoutineByPhrase(phrase: String): CustomRoutine?

    @Query("SELECT * FROM custom_routines WHERE id = :id")
    suspend fun getRoutineById(id: Int): CustomRoutine?
}
