package com.g7nightalarm.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reading: GlucoseReading): Long

    @Query("SELECT * FROM glucose_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun streamSince(since: Long): Flow<List<GlucoseReading>>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1")
    fun latestFlow(): Flow<GlucoseReading?>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): GlucoseReading?

    @Query("DELETE FROM glucose_readings WHERE timestamp < :before")
    suspend fun prune(before: Long): Int
}

