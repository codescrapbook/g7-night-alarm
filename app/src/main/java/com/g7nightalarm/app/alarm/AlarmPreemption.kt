package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver

/**
 * Preemption policy for starting the low ramp (below 70) in awake mode.
 * Urgent/ramp > low-warn > high > signal-lost.
 * Low ramp may start over HIGH, SIGNAL, or LOW_WARN; it should not re-start over LOW.
 */
object AlarmPreemption {
    fun shouldStartLowRamp(currentType: String?): Boolean {
        return when (currentType) {
            null -> true
            AlarmActionReceiver.TYPE_SIGNAL,
            AlarmActionReceiver.TYPE_HIGH,
            AlarmActionReceiver.TYPE_LOW_WARN -> true
            AlarmActionReceiver.TYPE_LOW -> false
            else -> true
        }
    }
}

