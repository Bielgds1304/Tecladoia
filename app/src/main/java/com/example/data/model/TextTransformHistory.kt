package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transform_history")
data class TextTransformHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val transformedText: String,
    val actionName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUndone: Boolean = false
)
