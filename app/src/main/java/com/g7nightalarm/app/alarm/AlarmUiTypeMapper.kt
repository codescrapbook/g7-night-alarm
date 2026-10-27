package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver

object AlarmUiTypeMapper {
    // Map AlarmEvent to UI type for the full-screen AlarmActivity
    // Returns null for events that should not show a full-screen alarm
    fun map(event: AlarmEvent): String? {
        return when (event) {
            is AlarmEvent.UrgentLow -> AlarmActionReceiver.TYPE_URGENT
            is AlarmEvent.Low -> AlarmActionReceiver.TYPE_LOW
            is AlarmEvent.High -> AlarmActionReceiver.TYPE_HIGH
            is AlarmEvent.NoData -> AlarmActionReceiver.TYPE_SIGNAL
            is AlarmEvent.RapidFall -> AlarmActionReceiver.TYPE_LOW
            is AlarmEvent.None -> null
        }
    }
}

