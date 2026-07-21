package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_commands")
data class VoiceCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val normalizedText: String,
    val intentName: String,
    val confidence: Float,
    val executionResult: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
