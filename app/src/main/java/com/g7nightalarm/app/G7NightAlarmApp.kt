package com.g7nightalarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class G7NightAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        maybeStartDebugGenerator()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitor = NotificationChannel(
                Channels.MONITOR_ID,
                getString(R.string.notification_channel_monitor_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_monitor_desc)
                setShowBadge(false)
            }
            val alarm = NotificationChannel(
                Channels.ALARM_ID,
                getString(R.string.notification_channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_alarm_desc)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(monitor)
            nm.createNotificationChannel(alarm)
        }
    }

    private fun maybeStartDebugGenerator() {
        if (BuildConfig.DEBUG) {
            try {
                val cls = Class.forName("com.g7nightalarm.app.debug.DebugData")
                val m = cls.getMethod("start", Context::class.java)
                m.invoke(null, applicationContext)
            } catch (_: Throwable) {
            }
        }
    }

    object Channels {
        const val MONITOR_ID = "g7_monitor"
        const val ALARM_ID = "g7_alarm"
    }
}

