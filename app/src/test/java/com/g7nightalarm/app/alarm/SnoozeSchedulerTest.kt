package com.g7nightalarm.app.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.g7nightalarm.app.schedule.SnoozeScheduler
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.time.ClockHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SnoozeSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        ClockHolder.clearDebugOffset()
        SnoozeScheduler.cancel(context)
    }

    @Test
    fun schedulesTheRearmForEachLength() {
        val now = 1_700_000_000_000L
        for (minutes in listOf(5, 10, 15, 20)) {
            SnoozeScheduler.cancel(context)
            val until = SnoozePolicy.untilWallMs(now, minutes)
            SnoozeScheduler.schedule(context, until)
            val scheduled = Shadows.shadowOf(alarmManager).scheduledAlarms
            assertEquals(1, scheduled.size)
            assertEquals(until, scheduled.single().triggerAtMs)
        }
    }

    @Test
    fun rescheduleAfterRebootRestoresAFutureSnooze() {
        val now = ClockHolder.nowWallMs()
        val until = SnoozePolicy.untilWallMs(now, 15)
        assertEquals(until, SnoozePolicy.triggerToReschedule(now + 1_000L, until))
        SnoozeScheduler.rescheduleIfPending(context, now + 1_000L, until)
        val scheduled = Shadows.shadowOf(alarmManager).scheduledAlarms
        assertEquals(until, scheduled.single().triggerAtMs)
    }

    @Test
    fun rescheduleAfterRebootDropsAnExpiredSnooze() {
        val until = ClockHolder.nowWallMs() - 1_000L
        assertNull(SnoozePolicy.triggerToReschedule(ClockHolder.nowWallMs(), until))
        SnoozeScheduler.schedule(context, ClockHolder.nowWallMs() + 60_000L)
        SnoozeScheduler.rescheduleIfPending(context, ClockHolder.nowWallMs(), until)
        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun cancelRemovesTheAlarmAndThePersistedSnooze() = runBlocking {
        val until = SnoozePolicy.untilWallMs(ClockHolder.nowWallMs(), 10)
        SettingsRepository(context).update { prefs ->
            prefs[SettingsKeys.snoozeUntilWallMs] = until
            prefs[SettingsKeys.snoozedAlarmType] = "low_warn"
            prefs[SettingsKeys.snoozedMgdl] = 76.0
            prefs[SettingsKeys.snoozeLengthMinutes] = 15
        }
        SnoozeScheduler.schedule(context, until)
        SnoozeController.cancel(context)
        val saved = SettingsRepository(context).flow.first()
        assertEquals(0L, saved.snoozeUntilWallMs)
        assertEquals("", saved.snoozedAlarmType)
        assertNull(saved.snoozedMgdl)
        assertEquals(15, saved.snoozeLengthMinutes)
        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun snoozeLengthPersistsInDataStore() = runBlocking {
        val repo = SettingsRepository(context)
        repo.update { it[SettingsKeys.snoozeLengthMinutes] = 20 }
        assertEquals(20, repo.flow.first().snoozeLengthMinutes)
        repo.update { it[SettingsKeys.snoozeLengthMinutes] = 7 }
        assertEquals(10, repo.flow.first().snoozeLengthMinutes)
    }

    @Test
    fun repeatArmReplacesTheScheduledTime() = runBlocking {
        val repo = SettingsRepository(context)
        repo.update { it[SettingsKeys.snoozeLengthMinutes] = 10 }
        val before = ClockHolder.nowWallMs()
        SnoozeController.arm(context, "low", 76.0, 10)
        val first = repo.flow.first().snoozeUntilWallMs
        assertTrue(first >= before + 10 * 60_000L)
        assertTrue(first <= before + 10 * 60_000L + 2_000L)
        assertEquals(first, Shadows.shadowOf(alarmManager).scheduledAlarms.single().triggerAtMs)
        val beforeRepeat = ClockHolder.nowWallMs()
        SnoozeController.arm(context, "low", 80.0, 5)
        val second = repo.flow.first().snoozeUntilWallMs
        assertTrue(second >= beforeRepeat + 5 * 60_000L)
        assertTrue(second <= beforeRepeat + 5 * 60_000L + 2_000L)
        assertEquals(second, Shadows.shadowOf(alarmManager).scheduledAlarms.single().triggerAtMs)
        assertEquals(80.0, repo.flow.first().snoozedMgdl!!, 0.01)
    }
}
