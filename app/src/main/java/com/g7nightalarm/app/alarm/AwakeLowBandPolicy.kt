package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver

/**
 * Policy for starting the awake-mode low-warning chime for 70–80 mg/dL band.
 * Never downgrades an in-progress alarm (low ramp/urgent/high/signal).
 * Only starts the chime when no other awake alarm is active.
 */
object AwakeLowBandPolicy {
    fun shouldStartLowWarnChime(
        currentType: String?,
        mgdl: Double,
        warnMin: Double,
        warnMax: Double
    ): Boolean {
        val inBand = mgdl in warnMin..warnMax
        if (!inBand) return false
        // Start when no alarm is active OR when a lower-priority alarm is active (HIGH or SIGNAL)
        return when (currentType) {
            null -> true
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL -> true
            // Do not start over existing LOW_WARN or LOW (equal or higher)
            else -> false
        }
    }

    fun isLowType(type: String?): Boolean = type == AlarmActionReceiver.TYPE_LOW
    fun isLowWarn(type: String?): Boolean = type == AlarmActionReceiver.TYPE_LOW_WARN
}

