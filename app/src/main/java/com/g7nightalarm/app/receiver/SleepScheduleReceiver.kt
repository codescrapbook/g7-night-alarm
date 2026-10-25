package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.schedule.SleepScheduler
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.voice.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SleepScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUTO_SLEEP) {
            // Ensure asynchronous work can complete even after onReceive returns
            val pending = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val repo = ModeRepository(appContext)
                    val settings = SettingsRepository(appContext)
                    val tts = TtsManager(appContext)
                    tts.ensure()
                    // Read current settings off the main thread
                    val cur = withContext(Dispatchers.IO) { settings.flow.first() }
                    if (!cur.sleepMode) {
                        repo.setMode(AppMode.SLEEP, ModeTrigger.SCHEDULE)
                        // Speak immediately so tests can observe the announcement deterministically
                        tts.speak("Turning on for sleep.", 60)
                    }
                    // Reschedule the next day's alarm
                    SleepScheduler.schedule(appContext, cur.autoSleepHour, cur.autoSleepMinute)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_AUTO_SLEEP = "com.g7nightalarm.AUTO_SLEEP"
    }
}

