package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CustomRoutine
import com.example.data.model.VoiceCommand

@Database(entities = [VoiceCommand::class, CustomRoutine::class], version = 1, exportSchema = false)
abstract class JaxonDatabase : RoomDatabase() {

    abstract fun voiceCommandDao(): VoiceCommandDao
    abstract fun customRoutineDao(): CustomRoutineDao

    companion object {
        @Volatile
        private var INSTANCE: JaxonDatabase? = null

        fun getDatabase(context: Context): JaxonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JaxonDatabase::class.java,
                    "jaxon_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
