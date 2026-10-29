package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import com.g7nightalarm.app.settings.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozePolicyTest {
    private val t0 = 1_700_000_000_000L

    @Test
    fun defaultLengthIsTenMinutes() {
        assertEquals(10, SnoozePolicy.DEFAULT_MINUTES)
        assertEquals(10, SettingsState().snoozeLengthMinutes)
        assertEquals(0L, SettingsState().snoozeUntilWallMs)
        assertEquals(listOf(5, 10, 15, 20), SnoozePolicy.lengthChoices)
    }

    @Test
    fun schedulesRearmAtTheChosenLength() {
        for (minutes in SnoozePolicy.lengthChoices) {
            assertEquals(t0 + minutes * 60_000L, SnoozePolicy.untilWallMs(t0, minutes))
            assertEquals("SNOOZE $minutes MIN", SnoozePolicy.buttonLabel(minutes))
        }
        assertEquals(t0 + 10 * 60_000L, SnoozePolicy.untilWallMs(t0, 7))
        assertEquals("SNOOZE 10 MIN", SnoozePolicy.buttonLabel(99))
    }

    @Test
    fun repeatSnoozeHasNoLimit() {
        var until = t0
        until = SnoozePolicy.untilWallMs(until, 10)
        until = SnoozePolicy.untilWallMs(until, 10)
        until = SnoozePolicy.untilWallMs(until, 5)
        assertEquals(t0 + 25 * 60_000L, until)
        assertTrue(SnoozePolicy.isActive(t0, until))
    }

    @Test
    fun inRangeReadingStillRearmsAsCheckIn() {
        val reading = GlucoseReading(
            timestamp = t0,
            mgdl = 110.0,
            slopeMgdlPerMin = 0.0,
            slopeName = "Flat",
            source = "test"
        )
        val event = AlarmEvaluator().evaluate(reading, null, t0, SettingsState())
        assertEquals(AlarmEvent.None, event)
        assertEquals(AlarmActionReceiver.TYPE_CHECKIN, SnoozePolicy.uiTypeForRearm(event))
        val until = SnoozePolicy.untilWallMs(t0, 10)
        assertEquals(
            SnoozePolicy.DuringSnooze.RearmDue,
            SnoozePolicy.duringSnooze(
                nowWallMs = until,
                untilWallMs = until,
                snoozedType = AlarmActionReceiver.TYPE_LOW_WARN,
                snoozedMgdl = 76.0,
                currentMgdl = 110.0,
                signalLost = false
            )
        )
    }

    @Test
    fun urgentDropOrSignalLostBreaksSnoozeImmediately() {
        val until = SnoozePolicy.untilWallMs(t0, 10)
        assertEquals(
            SnoozePolicy.DuringSnooze.BreakNow,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_LOW_WARN, 76.0, 55.0, false)
        )
        assertEquals(
            SnoozePolicy.DuringSnooze.BreakNow,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_LOW_WARN, 75.0, 69.0, false)
        )
        assertEquals(
            SnoozePolicy.DuringSnooze.BreakNow,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_HIGH, 200.0, 190.0, false)
        )
        assertTrue(SnoozePolicy.shouldBreakSnooze(AlarmActionReceiver.TYPE_HIGH, 200.0, 190.0, false))
        assertFalse(SnoozePolicy.shouldBreakSnooze(AlarmActionReceiver.TYPE_HIGH, 200.0, 191.0, false))
        assertEquals(
            SnoozePolicy.DuringSnooze.BreakNow,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_HIGH, 180.0, 180.0, signalLost = true)
        )
        assertEquals(
            SnoozePolicy.DuringSnooze.StayQuiet,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_LOW_WARN, 76.0, 78.0, false)
        )
        // The urgent reading Joe just snoozed must not cancel that snooze.
        assertEquals(
            SnoozePolicy.DuringSnooze.StayQuiet,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_URGENT, 58.0, 58.0, false)
        )
        assertEquals(
            SnoozePolicy.DuringSnooze.StayQuiet,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_URGENT, 58.0, 52.0, false)
        )
        assertEquals(
            SnoozePolicy.DuringSnooze.BreakNow,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_URGENT, 58.0, 48.0, false)
        )
    }

    @Test
    fun quietPeriodDoesNotBlockSnoozeRearm() {
        val clearedAt = t0 - 5 * 60_000L
        val quiet = AlarmPolicy.shouldSuppressBySnooze(t0, clearedAt, 30)
        assertTrue(quiet)
        assertFalse(SnoozePolicy.reAlarmAllowed(quietWouldSuppress = true, isSnoozeRearm = false))
        assertTrue(SnoozePolicy.reAlarmAllowed(quietWouldSuppress = true, isSnoozeRearm = true))
        val until = t0
        assertEquals(
            SnoozePolicy.DuringSnooze.RearmDue,
            SnoozePolicy.duringSnooze(t0, until, AlarmActionReceiver.TYPE_LOW_WARN, 76.0, 76.0, false)
        )
    }

    @Test
    fun rescheduleAfterRebootKeepsOnlyAFutureSnooze() {
        val until = SnoozePolicy.untilWallMs(t0, 15)
        assertEquals(until, SnoozePolicy.triggerToReschedule(t0 + 5_000L, until))
        assertNull(SnoozePolicy.triggerToReschedule(until + 1L, until))
        assertNull(SnoozePolicy.triggerToReschedule(t0, 0L))
    }

    @Test
    fun cancelClearsTheActiveSnooze() {
        assertFalse(SnoozePolicy.isActive(t0, 0L))
        assertFalse(SnoozePolicy.isDue(t0, 0L))
        val until = SnoozePolicy.untilWallMs(t0, 10)
        assertTrue(SnoozePolicy.isActive(t0, until))
        assertFalse(SnoozePolicy.isActive(t0, 0L))
    }
}
