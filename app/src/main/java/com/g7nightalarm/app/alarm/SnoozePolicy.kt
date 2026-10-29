package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver
import java.text.DateFormat
import java.util.Date

/**
 * User-chosen snooze. This is separate from the awake quiet windows in [AlarmPolicy].
 * The re-alarm is a wake-up: it is not swallowed by those windows.
 */
object SnoozePolicy {
    val lengthChoices: List<Int> = listOf(5, 10, 15, 20)
    const val DEFAULT_MINUTES = 10
    const val DROP_BREAK_MGDL = 10.0

    fun normalizeMinutes(minutes: Int): Int =
        if (minutes in lengthChoices) minutes else DEFAULT_MINUTES

    fun untilWallMs(nowWallMs: Long, minutes: Int): Long =
        nowWallMs + normalizeMinutes(minutes) * 60_000L

    fun isActive(nowWallMs: Long, untilWallMs: Long): Boolean =
        untilWallMs > nowWallMs

    fun isDue(nowWallMs: Long, untilWallMs: Long): Boolean =
        untilWallMs > 0L && nowWallMs >= untilWallMs

    /** After reboot or process death, reschedule only a still-future snooze. */
    fun triggerToReschedule(nowWallMs: Long, untilWallMs: Long): Long? =
        if (isActive(nowWallMs, untilWallMs)) untilWallMs else null

    enum class DuringSnooze { StayQuiet, BreakNow, RearmDue }

    /**
     * What to do when a reading arrives during a user snooze.
     * A due re-alarm wins, including inside the 30-minute 70–80 quiet period.
     * A worse event cancels the snooze and alarms immediately.
     */
    fun duringSnooze(
        nowWallMs: Long,
        untilWallMs: Long,
        snoozedType: String,
        snoozedMgdl: Double?,
        currentMgdl: Double?,
        signalLost: Boolean,
        urgentLowThreshold: Double = 70.0
    ): DuringSnooze {
        if (isDue(nowWallMs, untilWallMs)) return DuringSnooze.RearmDue
        if (shouldBreakSnooze(snoozedType, snoozedMgdl, currentMgdl, signalLost, urgentLowThreshold)) {
            return DuringSnooze.BreakNow
        }
        return DuringSnooze.StayQuiet
    }

    /**
     * A snooze re-alarm always fires, even inside the 30-minute 70–80 quiet period.
     */
    fun reAlarmAllowed(quietWouldSuppress: Boolean, isSnoozeRearm: Boolean): Boolean =
        isSnoozeRearm || !quietWouldSuppress

    /**
     * A worse event during snooze fires immediately and cancels the snooze.
     * - any urgent reading (at or below the urgent threshold, default 70)
     * - a reading below 70 when the snoozed alarm was the 70–80 low band
     * - a further drop of 10 mg/dL or more from the snoozed value
     * - signal lost
     */
    fun shouldBreakSnooze(
        snoozedType: String,
        snoozedMgdl: Double?,
        currentMgdl: Double?,
        signalLost: Boolean,
        urgentLowThreshold: Double = 70.0
    ): Boolean {
        if (signalLost) return true
        if (currentMgdl != null && currentMgdl <= urgentLowThreshold) return true
        val wasLowBand = snoozedType == AlarmActionReceiver.TYPE_LOW_WARN ||
            (snoozedMgdl != null && snoozedMgdl > urgentLowThreshold && snoozedMgdl <= 80.0 &&
                snoozedType == AlarmActionReceiver.TYPE_LOW)
        if (wasLowBand && currentMgdl != null && currentMgdl < urgentLowThreshold) return true
        if (snoozedMgdl != null && currentMgdl != null &&
            snoozedMgdl - currentMgdl >= DROP_BREAK_MGDL
        ) {
            return true
        }
        return false
    }

    /** In-range readings still wake Joe, as a check-in. Out-of-range uses the current alarm. */
    fun uiTypeForRearm(event: AlarmEvent): String =
        AlarmUiTypeMapper.map(event) ?: AlarmActionReceiver.TYPE_CHECKIN

    fun formatWakeTime(untilWallMs: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(untilWallMs))

    fun buttonLabel(minutes: Int): String = "SNOOZE ${normalizeMinutes(minutes)} MIN"
}
