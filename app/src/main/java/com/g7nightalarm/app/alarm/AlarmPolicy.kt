package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.mode.AppMode

object AlarmPolicy {
    fun isAwakeLowWarning(mgdl: Double, warnMin: Double, warnMax: Double): Boolean {
        return mgdl in warnMin..warnMax
    }

    fun shouldSuppressBySnooze(nowMs: Long, lastClearMs: Long, windowMinutes: Int): Boolean {
        if (lastClearMs <= 0L) return false
        val nextAllowed = lastClearMs + windowMinutes * 60_000L
        val suppressed = nowMs < nextAllowed
        // Emit at INFO to surface in CI logcats
        try {
            android.util.Log.i(
                "AlarmPolicy",
                "snoozeCheck suppressed=$suppressed now=$nowMs lastClear=$lastClearMs windowMin=$windowMinutes nextAllowed=$nextAllowed"
            )
        } catch (_: Throwable) {}
        return suppressed
    }

    fun shouldUseFullAlarm(
        event: AlarmEvent,
        mode: AppMode,
        safetyNetAlwaysFull: Boolean
    ): Boolean {
        return when (event) {
            is AlarmEvent.None -> false
            is AlarmEvent.UrgentLow -> true
            is AlarmEvent.NoData -> {
                if (safetyNetAlwaysFull && event.minutes >= 30) true
                else mode == AppMode.SLEEP
            }
            else -> mode == AppMode.SLEEP
        }
    }
}

