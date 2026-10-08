package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TextTransformHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM transform_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<TextTransformHistory>>

    @Query("SELECT * FROM transform_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTransform(): TextTransformHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TextTransformHistory): Long

    @Update
    suspend fun updateHistory(history: TextTransformHistory)

    @Query("DELETE FROM transform_history")
    suspend fun clearHistory()
}
