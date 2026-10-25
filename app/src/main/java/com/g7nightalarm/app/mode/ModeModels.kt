package com.g7nightalarm.app.mode

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppMode { SLEEP, AWAKE }
enum class ModeTrigger { BUTTON, VOICE, SCHEDULE, BOOT, UNKNOWN }

@Entity(tableName = "mode_change_history")
data class ModeChangeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mode: AppMode,
    val trigger: ModeTrigger,
)

