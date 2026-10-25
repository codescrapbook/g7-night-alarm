package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.alarm.AlarmController

/**
 * Debug-only receiver to start specific alarm audio flows for tests without going through xDrip.
 */
class TestAlarmStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = AlarmController(context)
        when (intent.action) {
            ACTION_START_AWAKE_LOW_WARN -> {
                val vol = intent.getIntExtra(EXTRA_AWAKE_VOL, 60)
                controller.startAwakeChimeLowWarning(vol)
            }
            ACTION_START_AWAKE_RAMP_LOW -> {
                val urgent = intent.getBooleanExtra(EXTRA_URGENT, false)
                val vol = intent.getIntExtra(EXTRA_AWAKE_VOL, 60)
                controller.startAwakeRampLow(vol, urgent)
            }
            ACTION_START_AWAKE_HIGH -> {
                val vol = intent.getIntExtra(EXTRA_AWAKE_VOL, 60)
                controller.startAwakeMusicHighDefault(vol)
            }
            ACTION_START_SIGNAL_LOST -> {
                val vol = intent.getIntExtra(EXTRA_AWAKE_VOL, 60)
                controller.startAwakeMusicSignalDefault(vol)
            }
        }
    }
    companion object {
        const val ACTION_START_AWAKE_LOW_WARN = "com.g7nightalarm.TEST_START_AWAKE_LOW_WARN"
        const val ACTION_START_AWAKE_RAMP_LOW = "com.g7nightalarm.TEST_START_AWAKE_RAMP_LOW"
        const val ACTION_START_AWAKE_HIGH = "com.g7nightalarm.TEST_START_AWAKE_HIGH"
        const val ACTION_START_SIGNAL_LOST = "com.g7nightalarm.TEST_START_SIGNAL_LOST"
        const val EXTRA_URGENT = "urgent"
        const val EXTRA_AWAKE_VOL = "awakeVol"
    }
}

