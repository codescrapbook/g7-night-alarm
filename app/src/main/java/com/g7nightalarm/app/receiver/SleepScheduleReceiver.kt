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
import kotlinx.coroutines.launch

class SleepScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUTO_SLEEP) {
            val repo = ModeRepository(context)
            val settings = SettingsRepository(context)
            val tts = TtsManager(context)
            tts.ensure()
            CoroutineScope(Dispatchers.IO).launch {
                val s = settings.flow
                // get current one-shot via first()
                val cur = kotlinx.coroutines.flow.first(s)
                if (!cur.sleepMode) {
                    repo.setMode(AppMode.SLEEP, ModeTrigger.SCHEDULE)
                    tts.speak("Turning on for sleep.", 60)
                }
                // reschedule next day
                SleepScheduler.schedule(context, cur.autoSleepHour, cur.autoSleepMinute)
            }
        }
    }

    companion object {
        const val ACTION_AUTO_SLEEP = "com.g7nightalarm.AUTO_SLEEP"
    }
}

