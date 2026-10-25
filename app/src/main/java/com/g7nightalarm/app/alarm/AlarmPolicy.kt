package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.mode.AppMode

object AlarmPolicy {
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

