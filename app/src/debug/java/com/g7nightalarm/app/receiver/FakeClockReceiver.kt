package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.g7nightalarm.app.time.ClockHolder

/**
 * Debug-only receiver to adjust the app's monotonic clock offset for tests.
 *
 * Actions:
 * - com.g7nightalarm.DEBUG_SET_CLOCK with extras:
 *   - "reset" (boolean): when true, clears the offset to 0
 *   - "offset_ms" (long): sets the absolute offset to this value
 *   - "advance_ms" (long): adds this delta to the current offset (positive or negative)
 */
class FakeClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_SET_CLOCK == intent.action) {
            val reset = intent.getBooleanExtra(EXTRA_RESET, false)
            if (reset) {
                ClockHolder.clearDebugOffset()
                Log.d(TAG, "FakeClock: reset offset to 0")
                return
            }
            if (intent.hasExtra(EXTRA_OFFSET_MS)) {
                val v = intent.getLongExtra(EXTRA_OFFSET_MS, 0L)
                ClockHolder.setDebugOffsetMs(v)
                Log.d(TAG, "FakeClock: set offset to $v ms")
            } else if (intent.hasExtra(EXTRA_ADVANCE_MS)) {
                val d = intent.getLongExtra(EXTRA_ADVANCE_MS, 0L)
                ClockHolder.addDebugOffsetMs(d)
                Log.d(TAG, "FakeClock: advanced offset by $d ms")
            }
        }
    }

    companion object {
        private const val TAG = "FakeClockReceiver"
        const val ACTION_SET_CLOCK = "com.g7nightalarm.DEBUG_SET_CLOCK"
        const val EXTRA_RESET = "reset"
        const val EXTRA_OFFSET_MS = "offset_ms"
        const val EXTRA_ADVANCE_MS = "advance_ms"
    }
}

