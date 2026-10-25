package com.g7nightalarm.app.mode

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Insert
    suspend fun insert(event: ModeChangeEvent)

    @Query("SELECT * FROM mode_change_history ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 50): Flow<List<ModeChangeEvent>>
}

