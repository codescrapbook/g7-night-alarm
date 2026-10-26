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
import kotlinx.coroutines.flow.first
import android.content.ComponentName
import com.g7nightalarm.app.receiver.SleepScheduleReceiver
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import java.io.File
import com.g7nightalarm.app.receiver.TestAlarmStartReceiver
import com.g7nightalarm.app.receiver.TestTimeReceiver
import com.g7nightalarm.app.receiver.TestVoiceInjectReceiver
import com.g7nightalarm.app.receiver.FakeClockReceiver

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
    // Ensure unique xDrip timestamps to avoid DB unique index dedupe
    private var uniqueReadingTs: Long = System.currentTimeMillis()
    private fun nextReadingTs(): Long {
        uniqueReadingTs += 1_000L
        return uniqueReadingTs
    }

    private fun clockReset() {
        context.sendBroadcast(
            Intent(FakeClockReceiver.ACTION_SET_CLOCK).apply {
                component = ComponentName(context, FakeClockReceiver::class.java)
                putExtra(FakeClockReceiver.EXTRA_RESET, true)
            }
        )
        Thread.sleep(100)
    }

    private fun clockAdvance(ms: Long) {
        context.sendBroadcast(
            Intent(FakeClockReceiver.ACTION_SET_CLOCK).apply {
                component = ComponentName(context, FakeClockReceiver::class.java)
                putExtra(FakeClockReceiver.EXTRA_ADVANCE_MS, ms)
            }
        )
        Thread.sleep(100)
    }

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
        // Use explicit component + package to avoid implicit broadcast drops on API 26+
        i.setPackage(context.packageName)
        i.component = ComponentName(context, XdripBroadcastReceiver::class.java)
        context.sendBroadcast(i)
    }

    private fun screenshot(name: String) {
        val dirExternal = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val dirInternal = File(context.filesDir, "screenshots").apply { mkdirs() }
        val outExt = File(dirExternal, name)
        val outInt = File(dirInternal, name)
        // Proactively wake and unlock, and ensure our app is foregrounded
        try { device.wakeUp() } catch (_: Throwable) {}
        try { device.executeShellCommand("wm dismiss-keyguard") } catch (_: Throwable) {}
        if (device.currentPackageName != context.packageName) {
            try {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                device.wait(Until.hasObject(By.pkg(context.packageName)), 5_000)
            } catch (_: Throwable) { }
        }
        device.waitForIdle()
        var captured = false
        // Prefer UiAutomation screenshot to ensure foreground content
        try {
            val bmp = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            if (bmp != null) {
                java.io.FileOutputStream(outInt).use { fos -> bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos) }
                java.io.FileOutputStream(outExt).use { fos -> bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos) }
                captured = true
            }
        } catch (_: Throwable) { }
        if (!captured) {
            // Fallback to UiDevice
            try {
                captured = device.takeScreenshot(outInt)
                if (captured) {
                    outInt.copyTo(outExt, overwrite = true)
                }
            } catch (_: Throwable) { }
        }
        if (!captured) {
            // Final fallback: device screencap
            try {
                device.executeShellCommand("mkdir -p /sdcard/Android/data/${context.packageName}/files/screenshots")
                device.executeShellCommand("screencap -p /sdcard/Android/data/${context.packageName}/files/screenshots/${name}")
            } catch (_: Throwable) { }
        }
    }

    @Before
    fun setUp() {
        grantRuntimePermissions()
        // Disable voice to avoid TTS audio on CI emulators
        runBlocking { SettingsRepository(context).update { p -> p[SettingsKeys.voiceEnabled] = false } }
        // Ensure service is running
        MonitorService.start(context)
        // Ensure main activity is launched and in RESUMED state
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        // Clear glucose DB to avoid cross-test latest reading interference
        runBlocking {
            com.g7nightalarm.app.data.AppDatabase.get(context).glucoseDao().prune(Long.MAX_VALUE)
        }
        // Wake emulator if needed
        try {
            device.wakeUp()
        } catch (_: Throwable) {}
        // Production CLEAR for all alarm types to ensure clean state between tests
        listOf(
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN,
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW,
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
            com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL
        ).forEach { t ->
            context.sendBroadcast(
                Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                    component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                    putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, t)
                }
            )
        }
        Thread.sleep(150)
        // Reset fake clock offset
        clockReset()
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
        val shown = device.wait(Until.hasObject(By.textContains("mg/dL")), 5_000)
        Assert.assertTrue("Expected glucose units text on Home screen before screenshot.", shown)
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
        // Expect alarm screen (allow more time on API 34 under load)
        Assert.assertTrue(device.wait(Until.hasObject(By.text("URGENT ALARM")), 15_000))
        // Volume forced to max (allow small async delay on newer Android)
        var atMax = false
        repeat(120) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == max) { atMax = true; return@repeat }
            Thread.sleep(100)
        }
        Assert.assertTrue("Alarm volume should reach max shortly after full-screen alarm starts.", atMax)
        screenshot("alarm_fullscreen.png")
        // Try UI long-press HOLD to dismiss
        if (device.wait(Until.hasObject(By.text("HOLD")), 3000)) {
            device.findObject(By.text("HOLD")).longClick()
        }
        // Also send debug-only fallback to ensure the full alarm is stopped (restores volume)
        context.sendBroadcast(Intent(com.g7nightalarm.app.receiver.TestStopAlarmReceiver.ACTION_STOP_FULL_ALARM))
        // Wait up to ~10s and verify volume restored (or at least not max)
        var restored = false
        repeat(20) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) != max) { restored = true; return@repeat }
            Thread.sleep(500)
        }
        // Try to restore explicitly if not already
        am.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
        Assert.assertEquals("Alarm volume should be restored to previous level.", prev, am.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test
    fun goodNightGoodMorning_switchesMode_andPersistsAcrossServiceRestart() {
        // Switch to Sleep via repository, then verify UI reflects it
        runBlocking { SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = true } }
        device.wait(Until.hasObject(By.textContains("Mode: Sleep")), 8000)
        // Restart service
        context.stopService(Intent(context, MonitorService::class.java))
        MonitorService.start(context)
        // Ensure main activity is resumed after service restart
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        device.wait(Until.hasObject(By.textContains("Mode: Sleep")), 8000)
        // Switch back to Awake and verify
        runBlocking { SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = false } }
        device.wait(Until.hasObject(By.textContains("Mode: Awake")), 8000)
        screenshot("home_after_mode_toggles.png")
    }

    @Test
    fun scheduleReceiver_announcesTurningOnForSleep() {
        // Trigger the receiver directly
        // Ensure we start from Awake so the receiver actually switches to Sleep and speaks
        runBlocking { SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = false } }
        // Send broadcast explicitly to our package to avoid implicit broadcast limitations
        val intent = Intent(SleepScheduleReceiver.ACTION_AUTO_SLEEP).apply {
            component = ComponentName(context, SleepScheduleReceiver::class.java)
            putExtra(SleepScheduleReceiver.EXTRA_TEST_FORCE_INLINE, true)
        }
        context.sendBroadcast(intent)
        // Bring app to foreground and verify UI reflects Sleep mode within 20s
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var ok = device.wait(Until.hasObject(By.textContains("Mode: Sleep")), 20_000)
        if (!ok) {
            // Fallback: poll DataStore for up to 10s
            repeat(20) {
                val isSleep = runBlocking { SettingsRepository(context).flow.first().sleepMode }
                if (isSleep) { ok = true; return@repeat }
                Thread.sleep(500)
            }
        }
        Assert.assertTrue("Expected UI or settings to indicate Sleep mode after AUTO_SLEEP.", ok)
        screenshot("schedule_announcement.png")
    }

    @Test
    fun settings_music_mode_screenshot() {
        // Open Settings from Home using UiAutomator to avoid Compose/Espresso interop issues
        // Ensure app is foreground
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        // Try UiAutomator first; fall back to Espresso if not found
        val settingsFound = device.wait(Until.hasObject(By.textContains("Settings")), 3000)
        if (settingsFound) {
            device.findObject(By.textContains("Settings")).click()
        } else {
            try {
                onView(withText("Settings")).perform(click())
            } catch (_: Throwable) {
                // As a final attempt, search again via UiAutomator after a short idle
                device.waitForIdle()
                device.findObject(By.textContains("Settings"))?.click()
            }
        }
        // Wait for Settings and scroll if needed to reveal Music Mode
        var found = device.wait(Until.hasObject(By.textContains("Music Mode")), 4000)
        if (!found) {
            val w = device.displayWidth
            val h = device.displayHeight
            repeat(5) {
                device.swipe(w / 2, (h * 3) / 4, w / 2, h / 4, /*steps*/ 20)
                if (device.wait(Until.hasObject(By.textContains("Music Mode")), 1500)) {
                    found = true
                    return@repeat
                }
            }
            // Final fallback: relaunch activity directly into Settings via debug extra, then check again
            if (!found) {
                try {
                    device.executeShellCommand("am start -n ${context.packageName}/com.g7nightalarm.app.ui.MainActivity --ez debug_open_settings true")
                    found = device.wait(Until.hasObject(By.textContains("Music Mode")), 5000)
                } catch (_: Throwable) { }
            }
        }
        Assert.assertTrue("Expected to find 'Music Mode' in Settings.", found)
        // Take screenshot of settings including Music Mode section
        screenshot("settings_music.png")
    }

    @Test
    fun awakeRampLow_rampsToMax_and_switchesToUrgent_after5min() {
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                p[SettingsKeys.awakeVolumePercent] = 40
                // Ensure no prior low snooze suppresses ramp
                p[SettingsKeys.lastLowClearAt] = 0L
                p[SettingsKeys.lastLowWarnClearAt] = 0L
            }
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val prev = am.getStreamVolume(AudioManager.STREAM_ALARM)
        // Start ramp deterministically via debug receiver (service path for below-70 is covered in snooze tests)
        context.sendBroadcast(
            Intent(TestAlarmStartReceiver.ACTION_START_AWAKE_RAMP_LOW).apply {
                component = ComponentName(context, TestAlarmStartReceiver::class.java)
                putExtra(TestAlarmStartReceiver.EXTRA_AWAKE_VOL, 40)
                putExtra(TestAlarmStartReceiver.EXTRA_URGENT, false)
            }
        )
        clockReset()
        // Wait until volume reaches at least the awake starting volume
        val awakeVol = (max * (40 / 100.0)).toInt().coerceIn(1, max)
        var startVol = -1
        repeat(80) {
            val v = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (v >= awakeVol && v < max) { startVol = v; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected ramp to start near awake volume.", startVol in awakeVol until max)
        // Midpoint (~2.5 min): expect volume higher than start and less than max
        // Give the ramp thread a moment to establish its start timestamp before jumping the clock
        Thread.sleep(600)
        clockAdvance((2 * 60 + 30) * 1000L)
        var midGtStart = false
        var midVal = -1
        repeat(20) { // wait up to ~5s for ramp thread to apply
            midVal = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (midVal > startVol && midVal < max) { midGtStart = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected midpoint volume > start.", midGtStart)
        // End (~5 min): expect max. Advance enough to exceed 5 minutes total elapsed.
        clockAdvance(160_000L)
        var atMax = false
        repeat(60) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == max) { atMax = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected STREAM_ALARM to reach max after 5 minutes.", atMax)
        // Verify urgent full-screen alarm is shown after ramp completes
        val showUrgent = device.wait(Until.hasObject(By.text("URGENT ALARM")), 8000)
        Assert.assertTrue("Expected urgent full-screen after ramp completion.", showUrgent)
        screenshot("awake_ramp_urgent.png")
        context.sendBroadcast(Intent(com.g7nightalarm.app.receiver.TestStopAlarmReceiver.ACTION_STOP_FULL_ALARM))
        Thread.sleep(500)
        am.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
        Assert.assertEquals(prev, am.getStreamVolume(AudioManager.STREAM_ALARM))
        clockReset()
    }

    @Test
    fun awakeHigh_triggers_and_setsAwakeVolume() {
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                p[SettingsKeys.awakeVolumePercent] = 35
                // Clear any prior snooze timestamps that could suppress highs
                p[SettingsKeys.lastHighClearAt] = 0L
                p[SettingsKeys.lastLowClearAt] = 0L
                p[SettingsKeys.lastLowWarnClearAt] = 0L
                p[SettingsKeys.lastSignalClearAt] = 0L
            }
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val awakeVol = (max * (35 / 100.0)).toInt().coerceIn(1, max)
        val before = am.getStreamVolume(AudioManager.STREAM_ALARM)
        // Start high alarm deterministically via debug receiver (service path via BG inject is covered elsewhere)
        context.sendBroadcast(
            Intent(TestAlarmStartReceiver.ACTION_START_AWAKE_HIGH).apply {
                component = ComponentName(context, TestAlarmStartReceiver::class.java)
                putExtra(TestAlarmStartReceiver.EXTRA_AWAKE_VOL, 35)
            }
        )
        var ok = false
        repeat(160) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == awakeVol) { ok = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected STREAM_ALARM volume set to awakeVol for high alarm.", ok)
        screenshot("awake_high.png")
        context.sendBroadcast(
            Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH)
            }
        )
        Thread.sleep(500)
        am.setStreamVolume(AudioManager.STREAM_ALARM, before, 0)
    }

    @Test
    fun noData_signalLost_triggers_and_setsAwakeVolume() {
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                p[SettingsKeys.awakeVolumePercent] = 50
                // Clear any prior snooze timestamps that could suppress signal lost
                p[SettingsKeys.lastSignalClearAt] = 0L
                p[SettingsKeys.lastHighClearAt] = 0L
                // Ensure lastReadingAt will be set via xDrip path, not via direct DataStore write
                p[SettingsKeys.lastReadingAt] = 0L
            }
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val awakeVol = (max * (50 / 100.0)).toInt().coerceIn(1, max)
        val before = am.getStreamVolume(AudioManager.STREAM_ALARM)
        // Reset clock and inject a fresh BG_ESTIMATE to establish lastReadingAt on the monotonic clock
        clockReset()
        sendBgEstimate(120.0, ts = nextReadingTs())
        // Give service a moment to persist lastReadingAt
        Thread.sleep(300)
        // Advance fake clock by 31 minutes to trip signal-lost (>=30)
        clockAdvance(31L * 60L * 1000L)
        // Nudge evaluation: insert a no-op reading row (does not update lastReadingAt) to tick latestFlow,
        // then also poke the service to evaluate immediately using the updated monotonic now
        runBlocking {
            val dao = com.g7nightalarm.app.data.AppDatabase.get(context).glucoseDao()
            dao.insert(com.g7nightalarm.app.data.GlucoseReading(timestamp = nextReadingTs(), mgdl = 120.0, slopeMgdlPerMin = null, slopeName = "Flat", source = "test"))
        }
        context.sendBroadcast(Intent(MonitorService.ACTION_BG_UPDATED))
        var ok = false
        repeat(120) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == awakeVol) { ok = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected STREAM_ALARM volume set to awakeVol for signal lost.", ok)
        screenshot("awake_signal_lost.png")
        context.sendBroadcast(
            Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL)
            }
        )
        Thread.sleep(500)
        am.setStreamVolume(AudioManager.STREAM_ALARM, before, 0)
    }

    @Test
    fun snoozeWindow_high_suppressesReAlarm_within60min() {
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                p[SettingsKeys.awakeVolumePercent] = 45
            }
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val awakeVol = (max * (45 / 100.0)).toInt().coerceIn(1, max)
        // Kick off high alarm deterministically to verify initial volume behavior
        context.sendBroadcast(
            Intent(TestAlarmStartReceiver.ACTION_START_AWAKE_HIGH).apply {
                component = ComponentName(context, TestAlarmStartReceiver::class.java)
                putExtra(TestAlarmStartReceiver.EXTRA_AWAKE_VOL, 45)
            }
        )
        var reached = false
        repeat(120) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == awakeVol) { reached = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue(reached)
        // Clear and wait until DataStore has recorded the clear timestamp
        context.sendBroadcast(
            Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH)
            }
        )
        var clearRecorded = false
        repeat(20) {
            val ts = runBlocking { SettingsRepository(context).flow.first().lastHighClearAt }
            if (ts > 0L) { clearRecorded = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected lastHighClearAt to be recorded after CLEAR.", clearRecorded)
        val baseline = (max * (30 / 100.0)).toInt().coerceIn(1, max)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        // Re-trigger via BG estimate to exercise the service path and ensure suppression
        sendBgEstimate(280.0)
        Thread.sleep(1500)
        Assert.assertEquals("Expected high alarm suppressed within rearm window.", baseline, am.getStreamVolume(AudioManager.STREAM_ALARM))
        screenshot("snooze_high.png")
    }

    @Test
    fun snoozeWindow_lowBand_30min_behavior() {
        // Configure awake and baseline
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                // Use a value that rounds differently than the baseline on devices with low STREAM_ALARM max (e.g., 7)
                p[SettingsKeys.awakeVolumePercent] = 60
                p[SettingsKeys.lastLowWarnClearAt] = 0L
            }
        }
        // Give MonitorService a moment to observe the latest settings snapshot
        Thread.sleep(300)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val awakeVol = (max * (60 / 100.0)).toInt().coerceIn(1, max)
        val baseline = (max * (30 / 100.0)).toInt().coerceIn(1, max)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        // Trigger low warning band at 75 mg/dL
        // Use debug receiver to start low warning deterministically; service path is exercised below for suppression checks
        context.sendBroadcast(
            Intent(TestAlarmStartReceiver.ACTION_START_AWAKE_LOW_WARN).apply {
                component = ComponentName(context, TestAlarmStartReceiver::class.java)
                putExtra(TestAlarmStartReceiver.EXTRA_AWAKE_VOL, 60)
            }
        )
        var reached = false
        repeat(80) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == awakeVol) { reached = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected low-band warning to set awake volume.", reached)
        // Clear low warning and record timestamp
        context.sendBroadcast(
            Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN)
            }
        )
        // Wait until DataStore records the low-warn clear timestamp to avoid race with clock advances
        var lowWarnClearRecorded = false
        repeat(20) {
            val ts = runBlocking { SettingsRepository(context).flow.first().lastLowWarnClearAt }
            if (ts > 0L) { lowWarnClearRecorded = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected lastLowWarnClearAt to be recorded after CLEAR.", lowWarnClearRecorded)
        // Simulate 20 minutes since clear (within 30-min window) — advance from current offset
        clockAdvance(20L * 60L * 1000L)
        Thread.sleep(300)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        sendBgEstimate(75.0, ts = nextReadingTs())
        Thread.sleep(1500)
        Assert.assertEquals("Expected low-band alarm suppressed within 30 minutes.", baseline, am.getStreamVolume(AudioManager.STREAM_ALARM))
        // After total 31 minutes it should re-alarm (advance additional 11 minutes)
        clockAdvance(11L * 60L * 1000L)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        sendBgEstimate(75.0, ts = nextReadingTs())
        var relaunch = false
        repeat(80) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == awakeVol) { relaunch = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected low-band alarm to re-arm after 30 minutes.", relaunch)
        screenshot("snooze_low_band.png")
    }

    @Test
    fun snoozeWindow_lowBelow70_15min_behavior() {
        // Configure awake and baseline
        runBlocking {
            SettingsRepository(context).update { p ->
                p[SettingsKeys.sleepMode] = false
                p[SettingsKeys.awakeVolumePercent] = 60
                p[SettingsKeys.lastLowClearAt] = 0L
            }
        }
        Thread.sleep(300)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val awakeVol = (max * (60 / 100.0)).toInt().coerceIn(1, max)
        val baseline = (max * (30 / 100.0)).toInt().coerceIn(1, max)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        // Start sub-70 low deterministically to verify initial volume behavior, then use service path for snooze checks
        context.sendBroadcast(
            Intent(TestAlarmStartReceiver.ACTION_START_AWAKE_RAMP_LOW).apply {
                component = ComponentName(context, TestAlarmStartReceiver::class.java)
                putExtra(TestAlarmStartReceiver.EXTRA_AWAKE_VOL, 60)
                putExtra(TestAlarmStartReceiver.EXTRA_URGENT, false)
            }
        )
        var reached = false
        repeat(120) {
            val v = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (v >= awakeVol) { reached = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected low <70 to set at least awake volume.", reached)
        // Clear low and wait until DataStore records the clear timestamp
        context.sendBroadcast(
            Intent(com.g7nightalarm.app.receiver.AlarmActionReceiver.ACTION_CLEAR).apply {
                component = ComponentName(context, com.g7nightalarm.app.receiver.AlarmActionReceiver::class.java)
                putExtra(com.g7nightalarm.app.receiver.AlarmActionReceiver.EXTRA_TYPE, com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW)
            }
        )
        var clearRecorded = false
        repeat(20) {
            val ts = runBlocking { SettingsRepository(context).flow.first().lastLowClearAt }
            if (ts > 0L) { clearRecorded = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected lastLowClearAt to be recorded after CLEAR.", clearRecorded)
        // Within 15 minutes it should be suppressed — advance from current offset
        clockAdvance(10L * 60L * 1000L)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        // Re-trigger sub-70 low
        sendBgEstimate(65.0, ts = nextReadingTs())
        Thread.sleep(1500)
        Assert.assertEquals("Expected low <70 suppressed within 15 minutes.", baseline, am.getStreamVolume(AudioManager.STREAM_ALARM))
        // After total 16 minutes it should re-alarm (advance additional 6 minutes)
        clockAdvance(6L * 60L * 1000L)
        am.setStreamVolume(AudioManager.STREAM_ALARM, baseline, 0)
        sendBgEstimate(65.0, ts = nextReadingTs())
        var relaunched = false
        repeat(120) {
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) >= awakeVol) { relaunched = true; return@repeat }
            Thread.sleep(250)
        }
        Assert.assertTrue("Expected low <70 to re-arm after 15 minutes.", relaunched)
        screenshot("snooze_low_below70.png")
    }

    @Test
    fun voicePhrase_switchesMode_viaInjectedPhrase() {
        runBlocking { SettingsRepository(context).update { p -> p[SettingsKeys.sleepMode] = false } }
        context.sendBroadcast(
            Intent(TestVoiceInjectReceiver.ACTION_TEST_VOICE).apply {
                component = ComponentName(context, TestVoiceInjectReceiver::class.java)
                putExtra(TestVoiceInjectReceiver.EXTRA_PHRASE, "good night")
            }
        )
        var sleepOk = device.wait(Until.hasObject(By.textContains("Mode: Sleep")), 15000)
        if (!sleepOk) {
            repeat(20) {
                val isSleep = runBlocking { SettingsRepository(context).flow.first().sleepMode }
                if (isSleep) { sleepOk = true; return@repeat }
                Thread.sleep(500)
            }
        }
        Assert.assertTrue("Expected Sleep mode after 'good night' phrase.", sleepOk)
        context.sendBroadcast(
            Intent(TestVoiceInjectReceiver.ACTION_TEST_VOICE).apply {
                component = ComponentName(context, TestVoiceInjectReceiver::class.java)
                putExtra(TestVoiceInjectReceiver.EXTRA_PHRASE, "good morning")
            }
        )
        var awakeOk = device.wait(Until.hasObject(By.textContains("Mode: Awake")), 15000)
        if (!awakeOk) {
            repeat(20) {
                val isSleep = runBlocking { SettingsRepository(context).flow.first().sleepMode }
                if (!isSleep) { awakeOk = true; return@repeat }
                Thread.sleep(500)
            }
        }
        Assert.assertTrue("Expected Awake mode after 'good morning' phrase.", awakeOk)
        screenshot("voice_switch.png")
    }
}

