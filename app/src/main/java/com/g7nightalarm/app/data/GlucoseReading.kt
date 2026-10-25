package com.g7nightalarm.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glucose_readings",
    indices = [Index(value = ["timestamp"], unique = true)]
)
data class GlucoseReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mgdl: Double,
    val slopeMgdlPerMin: Double?,
    val slopeName: String?,
    val source: String,
)

