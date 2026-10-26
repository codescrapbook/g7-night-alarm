package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.g7nightalarm.app.time.ClockHolder

/**
 * Debug-only receiver to control AlarmController's test clock.
 */
class TestTimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SET_NOW -> {
                val now = intent.getLongExtra(EXTRA_NOW_MS, -1L)
                if (now > 0) {
                    val offset = now - SystemClock.elapsedRealtime()
                    ClockHolder.setDebugOffsetMs(offset)
                }
            }
            ACTION_CLEAR_NOW -> {
                ClockHolder.clearDebugOffset()
            }
        }
    }
    companion object {
        const val ACTION_SET_NOW = "com.g7nightalarm.TEST_SET_NOW"
        const val ACTION_CLEAR_NOW = "com.g7nightalarm.TEST_CLEAR_NOW"
        const val EXTRA_NOW_MS = "nowMs"
    }
}

