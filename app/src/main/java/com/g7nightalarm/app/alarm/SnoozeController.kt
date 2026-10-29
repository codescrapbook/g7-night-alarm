package com.g7nightalarm.app.alarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.schedule.SnoozeScheduler
import com.g7nightalarm.app.service.MonitorService
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.time.ClockHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persists a user snooze, schedules the wake-up, and starts the re-alarm.
 */
object SnoozeController {
    const val ACTION_CHECK = "com.g7nightalarm.SNOOZE_CHECK"
    private val lock = Mutex()

    suspend fun arm(context: Context, alarmType: String, mgdl: Double?, minutes: Int) {
        lock.withLock {
            val repo = SettingsRepository(context)
            val until = SnoozePolicy.untilWallMs(ClockHolder.nowWallMs(), minutes)
            repo.update { p ->
                p[SettingsKeys.snoozeUntilWallMs] = until
                p[SettingsKeys.snoozedAlarmType] = alarmType
                if (mgdl != null) p[SettingsKeys.snoozedMgdl] = mgdl
                else p.remove(SettingsKeys.snoozedMgdl)
            }
            SnoozeScheduler.schedule(context, until)
            stopSounds(context)
        }
    }

    suspend fun cancel(context: Context) {
        lock.withLock {
            clearPersisted(context)
            SnoozeScheduler.cancel(context)
        }
    }

    /**
     * @param force true when AlarmManager itself fired (the wall-clock alarm is due).
     */
    suspend fun fire(context: Context, force: Boolean) {
        lock.withLock {
            val repo = SettingsRepository(context)
            val s = repo.flow.first()
            val now = ClockHolder.nowWallMs()
            if (s.snoozeUntilWallMs <= 0L) return
            if (!force && !SnoozePolicy.isDue(now, s.snoozeUntilWallMs)) return
            clearPersisted(context)
            SnoozeScheduler.cancel(context)
            val db = AppDatabase.get(context)
            val readings = db.glucoseDao().latest2()
            val latest = readings.getOrNull(0)
            val prev = readings.getOrNull(1)
            val evalNow = if (s.lastReadingAtMs > 0L) ClockHolder.nowMs() else System.currentTimeMillis()
            val event = AlarmEvaluator().evaluate(latest, prev, evalNow, s)
            val type = SnoozePolicy.uiTypeForRearm(event)
            val controller = AlarmController(context)
            if (s.sleepMode) {
                controller.startAlarm(type)
            } else {
                controller.openFullScreen(type)
                val vol = s.awakeVolumePercent
                val music = s.musicModeEnabled
                val lowUri = s.musicLowUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                val highUri = s.musicHighUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                val sigUri = s.musicSignalUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                when (event) {
                    is AlarmEvent.UrgentLow ->
                        if (music) controller.startAwakeMusicRampLow(vol, urgent = true, contentUri = lowUri)
                        else controller.startAwakeRampLow(vol, urgent = true)
                    is AlarmEvent.Low, is AlarmEvent.RapidFall ->
                        if (music) controller.startAwakeMusicRampLow(vol, urgent = false, contentUri = lowUri)
                        else controller.startAwakeRampLow(vol, urgent = false)
                    is AlarmEvent.High ->
                        if (music) {
                            if (highUri != null) controller.startAwakeMusicFromUri(vol, highUri)
                            else controller.startAwakeMusicHighDefault(vol)
                        } else controller.startAwakeChimeHigh(vol)
                    else ->
                        if (music) {
                            if (sigUri != null) controller.startAwakeMusicFromUri(vol, sigUri)
                            else controller.startAwakeMusicSignalDefault(vol)
                        } else controller.startAwakeChimeSignal(vol)
                }
            }
        }
    }

    private suspend fun clearPersisted(context: Context) {
        SettingsRepository(context).update { p ->
            p[SettingsKeys.snoozeUntilWallMs] = 0L
            p[SettingsKeys.snoozedAlarmType] = ""
            p.remove(SettingsKeys.snoozedMgdl)
        }
    }

    private fun stopSounds(context: Context) {
        AlarmController.stopActiveSounds(context)
        context.sendBroadcast(Intent(MonitorService.ACTION_ALARM_STOPPED).apply {
            setPackage(context.packageName)
        })
    }
}
