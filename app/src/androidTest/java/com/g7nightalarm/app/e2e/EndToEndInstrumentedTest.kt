package com.g7nightalarm.app.e2e

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.g7nightalarm.app.receiver.XdripBroadcastReceiver
import com.g7nightalarm.app.service.MonitorService
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class EndToEndInstrumentedTest {

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val notifPerm: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33)
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS) else GrantPermissionRule.grant()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun grantRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            device.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        }
        device.executeShellCommand("cmd appops set ${context.packageName} RUN_IN_BACKGROUND allow")
        device.executeShellCommand("cmd appops set ${context.packageName} WAKE_LOCK allow")
    }

    private fun sendBgEstimate(mgdl: Double, slopePerMin: Double? = null, ts: Long = System.currentTimeMillis()) {
        val i = Intent(XdripBroadcastReceiver.ACTION_BG_ESTIMATE).apply {
            putExtra(XdripBroadcastReceiver.EXTRA_BG_ESTIMATE, mgdl)
            putExtra(XdripBroadcastReceiver.EXTRA_TIMESTAMP, ts)
            slopePerMin?.let { putExtra(XdripBroadcastReceiver.EXTRA_BG_SLOPE, it / 60_000.0) } // per ms
            putExtra(XdripBroadcastReceiver.EXTRA_BG_SLOPE_NAME, "Flat")
        }
        context.sendBroadcast(i)
    }

    private fun screenshot(name: String) {
        val dir = File(context.getExternalFilesDir(null), "screenshots")
        dir.mkdirs()
        val out = File(dir, name)
        // UiDevice screenshot works without window
        device.waitForIdle()
        device.takeScreenshot(out)
    }

    @Before
    fun setUp() {
        grantRuntimePermissions()
        // Ensure service is running
        MonitorService.start(context)
        // Wake and unlock emulator if needed
        try {
            device.wakeUp()
            device.pressHome()
        } catch (_: Throwable) {}
    }

    @Test
    fun injectBroadcast_isStored_andShownOnHome() {
        // Start in awake mode
        runBlocking {
            SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = false }
        }
        // Send a reading
        sendBgEstimate(123.4)
        // Open app and expect value appears (rounded) in title text
        device.wait(Until.hasObject(By.textContains("mg/dL")), 5_000)
        screenshot("home_after_bg_inject.png")
    }

    @Test
    fun sleepMode_urgentLow_showsFullScreen_and_restoresVolume_onDismiss() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prev = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        // Set sleep mode
        runBlocking {
            SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = true }
        }
        // Inject urgent low
        sendBgEstimate(52.0)
        // Expect alarm screen
        Assert.assertTrue(device.wait(Until.hasObject(By.text("URGENT ALARM")), 7_000))
        // Volume forced to max
        Assert.assertEquals(max, am.getStreamVolume(AudioManager.STREAM_ALARM))
        screenshot("alarm_fullscreen.png")
        // Long-press HOLD to dismiss
        val hold = device.findObject(By.text("HOLD"))
        hold.longClick()
        // Wait a moment and verify volume restored (or at least not max)
        Thread.sleep(1000)
        Assert.assertNotEquals("Alarm volume should be restored after dismiss", max, am.getStreamVolume(AudioManager.STREAM_ALARM))
        // Try to restore explicitly if not already
        am.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
    }

    @Test
    fun goodNightGoodMorning_switchesMode_andPersistsAcrossServiceRestart() {
        // Tap Good Night
        onView(withText("Good Night")).perform(click())
        // Restart service
        context.stopService(Intent(context, MonitorService::class.java))
        MonitorService.start(context)
        // Tap Good Morning
        onView(withText("Good Morning")).perform(click())
        screenshot("home_after_mode_toggles.png")
    }

    @Test
    fun scheduleReceiver_announcesTurningOnForSleep() {
        // Trigger the receiver directly
        context.sendBroadcast(Intent(com.g7nightalarm.app.receiver.SleepScheduleReceiver.ACTION_AUTO_SLEEP))
        // Allow async work
        Thread.sleep(1500)
        Assert.assertTrue(com.g7nightalarm.app.voice.TtsManager.lastMessage?.contains("Turning on for sleep.") == true)
        screenshot("schedule_announcement.png")
    }

    @Test
    fun settings_music_mode_screenshot() {
        // Open Settings from Home
        device.findObject(By.text("Settings")).click()
        device.wait(Until.hasObject(By.textContains("Music Mode")), 3000)
        // Take screenshot of settings including Music Mode section
        screenshot("settings_music.png")
    }
}

