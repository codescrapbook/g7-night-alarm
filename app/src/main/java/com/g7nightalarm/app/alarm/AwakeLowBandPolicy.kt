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
        // Only when nothing else is active
        return currentType == null
    }

    fun isLowType(type: String?): Boolean = type == AlarmActionReceiver.TYPE_LOW
    fun isLowWarn(type: String?): Boolean = type == AlarmActionReceiver.TYPE_LOW_WARN
}

