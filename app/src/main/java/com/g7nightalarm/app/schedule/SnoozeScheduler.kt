package com.g7nightalarm.app.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.g7nightalarm.app.receiver.SnoozeReceiver
import com.g7nightalarm.app.ui.MainActivity

/**
 * Wall-clock snooze wake-up. [AlarmManager.setAlarmClock] fires in Doze and shows
 * the alarm-clock indicator. If that is not allowed, fall back to
 * [AlarmManager.setExactAndAllowWhileIdle].
 */
object SnoozeScheduler {
    private const val REQUEST = 202

    fun schedule(context: Context, triggerAtWallMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = fireIntent(context)
        val show = PendingIntent.getActivity(
            context,
            REQUEST + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtWallMs, show), fire)
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtWallMs, fire)
            }
        } catch (se: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallMs, fire)
                } else {
                    @Suppress("DEPRECATION")
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtWallMs, fire)
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(fireIntent(context))
    }

    fun rescheduleIfPending(context: Context, nowWallMs: Long, untilWallMs: Long) {
        val trigger = com.g7nightalarm.app.alarm.SnoozePolicy.triggerToReschedule(nowWallMs, untilWallMs)
        if (trigger != null) schedule(context, trigger) else cancel(context)
    }

    private fun fireIntent(context: Context): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
