package com.g7nightalarm.app.robo

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.g7nightalarm.app.receiver.BootReceiver
import com.g7nightalarm.app.receiver.SleepScheduleReceiver
import com.g7nightalarm.app.schedule.SleepScheduler
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows

class BootAndScheduleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun bootReceiverStartsAndSchedules() {
        // Ensure scheduling is called by observing AlarmManager shadow
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAm = Shadows.shadowOf(am)
        val receiver = BootReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        // We can't assert service start easily here, but we can check that at least one alarm was scheduled later in onReceive path
        assertTrue(shadowAm.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun sleepScheduleReceiverReschedules() = runBlocking {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAm = Shadows.shadowOf(am)
        val repo = SettingsRepository(context)
        // trigger receiver
        val receiver = SleepScheduleReceiver()
        receiver.onReceive(context, Intent(SleepScheduleReceiver.ACTION_AUTO_SLEEP))
        assertTrue(shadowAm.scheduledAlarms.isNotEmpty())
    }
}

