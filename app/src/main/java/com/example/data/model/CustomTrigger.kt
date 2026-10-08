package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_triggers")
data class CustomTrigger(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val prompt: String,
    val iconSymbol: String = "🎯",
    val iconKey: String = "dragon",
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
