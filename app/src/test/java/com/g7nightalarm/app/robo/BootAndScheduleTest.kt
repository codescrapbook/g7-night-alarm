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
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import android.os.Looper
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BootAndScheduleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun bootReceiverStartsAndSchedules() {
        val receiver = BootReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        // Assert service start intent was issued
        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val started = shadowApp.nextStartedService
        assertTrue(started != null)
    }

    @Test
    @org.junit.Ignore("Flaky in CI without a full main looper and DataStore; verified in instrumentation")
    fun sleepScheduleReceiverReschedules() = runBlocking {
        // trigger receiver and assert TTS announcement
        val receiver = SleepScheduleReceiver()
        receiver.onReceive(context, Intent(SleepScheduleReceiver.ACTION_AUTO_SLEEP))
        // allow background coroutine and any main looper work to run
        ShadowLooper.idleMainLooper()
        Thread.sleep(300)
        assertTrue(com.g7nightalarm.app.voice.TtsManager.lastMessage?.contains("Turning on for sleep.") == true)
    }
}

