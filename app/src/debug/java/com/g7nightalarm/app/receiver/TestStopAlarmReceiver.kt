package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.service.MonitorService

/**
 * Debug-only receiver to forcibly stop the full-screen alarm for instrumented tests.
 */
class TestStopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_STOP_FULL_ALARM) {
            val controller = AlarmController(context)
            controller.stopAlarm()
            // notify service so it can resume voice listening
            context.sendBroadcast(Intent(MonitorService.ACTION_ALARM_STOPPED))
        }
    }
    companion object {
        const val ACTION_STOP_FULL_ALARM = "com.g7nightalarm.STOP_FULL_ALARM"
    }
}
