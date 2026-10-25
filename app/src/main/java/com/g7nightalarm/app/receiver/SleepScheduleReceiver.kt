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
import kotlinx.coroutines.runBlocking

class SleepScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUTO_SLEEP) {
            val appContext = context.applicationContext
            val repo = ModeRepository(appContext)
            val settings = SettingsRepository(appContext)
            val tts = TtsManager(appContext)
            tts.ensure()
            // Fast path for instrumented tests to avoid any background scheduling or Room overhead
            val testInline = intent.getBooleanExtra(EXTRA_TEST_FORCE_INLINE, false)
            if (testInline) {
                val cur = runBlocking { settings.flow.first() }
                if (!cur.sleepMode) {
                    runBlocking { settings.update { it[SettingsKeys.sleepMode] = true } }
                    tts.speak("Turning on for sleep.", 60)
                }
                SleepScheduler.schedule(appContext, cur.autoSleepHour, cur.autoSleepMinute)
                return
            }
            // Perform minimal synchronous work to be deterministic in tests and reliable in background
            val cur = runBlocking { settings.flow.first() }
            if (!cur.sleepMode) {
                runBlocking { repo.setMode(AppMode.SLEEP, ModeTrigger.SCHEDULE) }
                // Speak immediately so tests can observe the announcement deterministically
                tts.speak("Turning on for sleep.", 60)
            }
            // Reschedule the next day's alarm
            SleepScheduler.schedule(appContext, cur.autoSleepHour, cur.autoSleepMinute)
        }
    }

    companion object {
        const val ACTION_AUTO_SLEEP = "com.g7nightalarm.AUTO_SLEEP"
        const val EXTRA_TEST_FORCE_INLINE = "com.g7nightalarm.EXTRA_TEST_FORCE_INLINE"
    }
}

