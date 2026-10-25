package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.schedule.SleepScheduler
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.service.MonitorService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start monitor service on boot and when app is updated
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MonitorService.start(context)
                // Reschedule auto-sleep
                val settings = SettingsRepository(context)
                GlobalScope.launch {
                    val s = settings.flow.first()
                    SleepScheduler.schedule(context, s.autoSleepHour, s.autoSleepMinute)
                }
            }
        }
    }
}

