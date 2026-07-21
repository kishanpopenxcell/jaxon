package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_routines")
data class CustomRoutine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerPhrase: String,
    val actionsJson: String, // Comma-separated list or JSON array of textual commands (e.g. "open gmail, open calendar, increase volume")
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
