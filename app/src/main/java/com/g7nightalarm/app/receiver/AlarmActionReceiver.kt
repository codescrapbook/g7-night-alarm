package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLEAR) {
            val type = intent.getStringExtra(EXTRA_TYPE) ?: ""
            // stop any running audio
            val controller = AlarmController(context)
            controller.stopAwakeChime()
            controller.stopAwakeMusic()
            GlobalScope.launch {
                val repo = SettingsRepository(context)
                repo.update { p ->
                    val now = System.currentTimeMillis()
                    when (type) {
                        TYPE_LOW_WARN -> p[SettingsKeys.lastLowWarnClearAt] = now
                        TYPE_LOW -> p[SettingsKeys.lastLowClearAt] = now
                        TYPE_HIGH -> p[SettingsKeys.lastHighClearAt] = now
                        TYPE_SIGNAL -> p[SettingsKeys.lastSignalClearAt] = now
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_CLEAR = "com.g7nightalarm.CLEAR_ALARM"
        const val EXTRA_TYPE = "type"
        const val TYPE_LOW_WARN = "low_warn"
        const val TYPE_LOW = "low"
        const val TYPE_HIGH = "high"
        const val TYPE_SIGNAL = "signal"
    }
}

