package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomTrigger
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {
    @Query("SELECT * FROM custom_triggers ORDER BY sortOrder ASC, id DESC")
    fun getAllTriggers(): Flow<List<CustomTrigger>>

    @Query("SELECT * FROM custom_triggers WHERE isEnabled = 1 ORDER BY sortOrder ASC, id DESC")
    fun getActiveTriggers(): Flow<List<CustomTrigger>>

    @Query("SELECT * FROM custom_triggers WHERE isEnabled = 1 ORDER BY sortOrder ASC, id DESC")
    suspend fun getActiveTriggersSync(): List<CustomTrigger>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: CustomTrigger): Long

    @Update
    suspend fun updateTrigger(trigger: CustomTrigger)

    @Delete
    suspend fun deleteTrigger(trigger: CustomTrigger)

    @Query("DELETE FROM custom_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
