package com.g7nightalarm.app.mode

import android.content.Context
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModeRepository(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val settings = SettingsRepository(context)

    val currentModeFlow: Flow<AppMode> = settings.flow.map { if (it.sleepMode) AppMode.SLEEP else AppMode.AWAKE }
    val historyFlow: Flow<List<ModeChangeEvent>> = db.modeDao().recent()

    suspend fun setMode(mode: AppMode, trigger: ModeTrigger) {
        settings.update { prefs ->
            prefs[SettingsKeys.sleepMode] = (mode == AppMode.SLEEP)
        }
        db.modeDao().insert(
            ModeChangeEvent(
                timestamp = System.currentTimeMillis(),
                mode = mode,
                trigger = trigger
            )
        )
    }
}

