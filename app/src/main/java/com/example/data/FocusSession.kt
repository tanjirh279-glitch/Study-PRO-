package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationSeconds: Long,
    val targetSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String, // "POMODORO", "SHORT_BREAK", "LONG_BREAK"
    val completed: Boolean
)
