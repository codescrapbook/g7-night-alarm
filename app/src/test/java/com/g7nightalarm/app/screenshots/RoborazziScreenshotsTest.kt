package com.g7nightalarm.app.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.preview.AlarmScreens
import com.g7nightalarm.app.ui.screens.HomeScreen
import com.g7nightalarm.app.ui.screens.SettingsScreen
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import com.g7nightalarm.app.ui.screens.computeChartSeries

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-land-mdpi")
@GraphicsMode(NATIVE)
class RoborazziScreenshotsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyInDebug() {
        assumeTrue(com.g7nightalarm.app.BuildConfig.DEBUG)
    }
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File("build/outputs/roborazzi/screens")

    private fun buildHistory(hours: Int, endNow: Long, endMgdl: Double, slopeName: String): List<GlucoseReading> {
        val points = hours * 12
        val list = ArrayList<GlucoseReading>(points + 2)
        for (i in 0 until points) {
            val t = endNow - (points - i) * 5L * 60L * 1000L
            val base = 120.0 + 20.0 * kotlin.math.sin(i / 8.0)
            list.add(GlucoseReading(timestamp = t, mgdl = base, slopeMgdlPerMin = null, slopeName = "Flat", source = "test"))
        }
        list.add(GlucoseReading(timestamp = endNow - 5 * 60_000L, mgdl = (endMgdl - 20.0), slopeMgdlPerMin = null, slopeName = slopeName, source = "test"))
        list.add(GlucoseReading(timestamp = endNow, mgdl = endMgdl, slopeMgdlPerMin = null, slopeName = slopeName, source = "test"))
        return list
    }

    private fun renderAndSavePngCompose(name: String, content: @Composable () -> Unit) {
        outDir.mkdirs()
        composeRule.setContent { G7Theme { content() } }
        composeRule.waitForIdle()
        val file = File(outDir, name)
        val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(file).use { fos ->
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun renderAndSavePngActivity(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent { G7Theme { content() } }
        repeat(20) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            try { Thread.sleep(75) } catch (_: Throwable) {}
        }
        outDir.mkdirs()
        val file = File(outDir, name)
        val bitmap = activity.window.decorView.drawToBitmap()
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }
    private fun renderAndSavePngActivityFontScale(name: String, fontScale: Float, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            G7Theme {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale)
                ) { content() }
            }
        }
        repeat(20) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            try { Thread.sleep(75) } catch (_: Throwable) {}
        }
        outDir.mkdirs()
        val file = File(outDir, name)
        val bitmap = activity.window.decorView.drawToBitmap()
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun seedReadings(hours: Int = 24) = runBlocking {
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        val points = hours * 12
        for (i in 0 until points) {
            val t = now - (points - i) * 5L * 60L * 1000L
            val base = 120.0 + 20.0 * sin(i / 8.0)
            db.insert(
                GlucoseReading(
                    timestamp = t,
                    mgdl = base,
                    slopeMgdlPerMin = null,
                    slopeName = "Flat",
                    source = "test"
                )
            )
        }
    }

    // Font scale 2.0 screenshots (tablet)
    @Test
    fun fs2_tablet_home_settings_and_alarms() = runBlocking {
        val now = System.currentTimeMillis()
        val latest58 = GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test")
        val latest76 = GlucoseReading(timestamp = now - 60_000L, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test")
        val latest260 = GlucoseReading(timestamp = now - 60_000L, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test")
        val latest120 = GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "→", source = "test")
        setUnits(Units.MG_DL)
        // Home and Settings
        renderAndSavePngActivityFontScale("fs2_home_awake_mgdl.png", 2f) { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}) } }
        renderAndSavePngActivityFontScale("fs2_settings.png", 2f) { Box(Modifier.size(1280.dp, 800.dp)) { SettingsScreen(onBack = {}) } }
        // Alarms
        renderAndSavePngActivityFontScale("fs2_alarm_urgent.png", 2f) {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest58,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_alarm_low_warn.png", 2f) {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest76,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_alarm_low.png", 2f) {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest76,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_alarm_high.png", 2f) {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest260,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_alarm_signal_lost.png", 2f) {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest120,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }

    // Font scale 2.0 screenshots (phone)
    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun fs2_phone_home_settings_and_alarms() = runBlocking {
        val now = System.currentTimeMillis()
        val latest58 = GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test")
        val latest76 = GlucoseReading(timestamp = now - 60_000L, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test")
        val latest260 = GlucoseReading(timestamp = now - 60_000L, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test")
        val latest120 = GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "→", source = "test")
        setUnits(Units.MG_DL)
        renderAndSavePngActivityFontScale("fs2_phone_home_sleep_mgdl.png", 2f) { Box(Modifier.size(411.dp, 891.dp)) { HomeScreen(onOpenSettings = {}, previewIsSleep = true) } }
        renderAndSavePngActivityFontScale("fs2_phone_settings.png", 2f) { Box(Modifier.size(411.dp, 891.dp)) { SettingsScreen(onBack = {}) } }
        renderAndSavePngActivityFontScale("fs2_phone_alarm_urgent.png", 2f) {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest58,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_phone_alarm_low_warn.png", 2f) {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest76,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_phone_alarm_low.png", 2f) {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest76,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_phone_alarm_high.png", 2f) {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest260,
                    previewUnits = Units.MG_DL
                )
            }
        }
        renderAndSavePngActivityFontScale("fs2_phone_alarm_signal_lost.png", 2f) {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest120,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }
    private fun setUnits(units: Units) = runBlocking {
        val repo = SettingsRepository(context)
        repo.update { p -> p[SettingsKeys.units] = units.name }
    }

    private fun setMode(sleep: Boolean) = runBlocking {
        val repo = SettingsRepository(context)
        repo.update { p -> p[SettingsKeys.sleepMode] = sleep }
    }

    private fun setMusicMode(enabled: Boolean) = runBlocking {
        val repo = SettingsRepository(context)
        repo.update { p -> p[SettingsKeys.musicModeEnabled] = enabled }
    }

    @Test
    fun home_awake_mgdl() = runBlocking {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.2, slopeName = "↗", source = "test")
        setUnits(Units.MG_DL)
        setMode(false)
        val hist = buildHistory(3, now, 142.0, "↗")
        // Assert seeded history last equals latest for the chart
        val series = computeChartSeries(hist, latest)
        org.junit.Assert.assertEquals(142.0, series.last().mgdl, 0.0)
        org.junit.Assert.assertEquals(now, series.last().timestamp)
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("142 mg/dL", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        renderAndSavePngActivity("home_awake_mgdl.png") { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } }
    }

    @Test
    fun home_sleep_mgdl() = runBlocking {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "Flat", source = "test")
        val hist = buildHistory(3, now, 120.0, "Flat")
        val series = computeChartSeries(hist, latest)
        org.junit.Assert.assertEquals(120.0, series.last().mgdl, 0.0)
        org.junit.Assert.assertEquals(now, series.last().timestamp)
        setUnits(Units.MG_DL)
        setMode(true)
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist, previewIsSleep = true) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("Mode: Sleep", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        renderAndSavePngActivity("home_sleep_mgdl.png") { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist, previewIsSleep = true) } }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun home_awake_mmol_port() = runBlocking {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test")
        setUnits(Units.MMOL_L)
        setMode(false)
        val hist = buildHistory(3, now, 76.0, "↘")
        val series = computeChartSeries(hist, latest)
        org.junit.Assert.assertEquals(76.0, series.last().mgdl, 0.0)
        org.junit.Assert.assertEquals(now, series.last().timestamp)
        renderAndSavePngActivity("home_awake_mmol_port.png") { Box(Modifier.size(800.dp, 1280.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } }
    }

    @Test
    fun settings() {
        renderAndSavePngActivity("settings.png") { Box(Modifier.size(1280.dp, 800.dp)) { SettingsScreen(onBack = {}) } }
    }

    @Test
    fun settings_music() {
        setMusicMode(true)
        renderAndSavePngActivity("settings_music.png") {
            Box(Modifier.size(1280.dp, 800.dp)) {
                SettingsScreen(onBack = {})
                androidx.compose.material3.Text(
                    "MUSIC MODE ON",
                    modifier = androidx.compose.ui.Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                    color = androidx.compose.ui.graphics.Color.Magenta
                )
            }
        }
    }

    @Test
    fun home_awake_76_mgdl() = runBlocking {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test")
        setUnits(Units.MG_DL)
        setMode(false)
        val hist = buildHistory(3, now, 76.0, "↘")
        val series = computeChartSeries(hist, latest)
        org.junit.Assert.assertEquals(76.0, series.last().mgdl, 0.0)
        org.junit.Assert.assertEquals(now, series.last().timestamp)
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        renderAndSavePngActivity("home_awake_76_mgdl.png") { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } }
    }

    @Test
    fun alarm_urgent_screen() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 6 * 60_000L, mgdl = 65.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
            db.insert(GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("URGENT ALARM").assertExists()
                composeRule.onNodeWithText("58 mg/dL", substring = true).assertExists()
                true
            } catch (_: AssertionError) { false }
        }
        composeRule.onNodeWithText("URGENT ALARM").assertExists()
        composeRule.onNodeWithText("58 mg/dL", substring = true).assertExists()
        composeRule.onNodeWithText("↘", substring = true).assertExists()
        composeRule.onNodeWithText("min ago", substring = true).assertExists()
        composeRule.onNodeWithText("HOLD").assertExists()
        // Render via activity using previewLatest to ensure deterministic text
        renderAndSavePngActivity("alarm_urgent.png") {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }

    @Test
    fun alarm_low_warn_screen() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 79.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists(); true
            } catch (_: AssertionError) { false }
        }
        composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists()
        composeRule.onNodeWithText("↘", substring = true).assertExists()
        composeRule.onNodeWithText("min ago", substring = true).assertExists()
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_low_warn.png") {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }

    @Test
    fun alarm_high_screen() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 240.0, slopeMgdlPerMin = null, slopeName = "↗", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("260 mg/dL", substring = true).assertExists(); true
            } catch (_: AssertionError) { false }
        }
        composeRule.onNodeWithText("260 mg/dL", substring = true).assertExists()
        composeRule.onNodeWithText("↗", substring = true).assertExists()
        composeRule.onNodeWithText("min ago", substring = true).assertExists()
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_high.png") {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }

    @Test
    fun alarm_signal_lost_screen() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = null, slopeName = "→", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "→", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("120 mg/dL", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
        composeRule.onNodeWithText("120 mg/dL", substring = true).assertExists()
        composeRule.onNodeWithText("→", substring = true).assertExists()
        composeRule.onNodeWithText("min ago", substring = true).assertExists()
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_signal_lost.png") {
            Box(Modifier.size(1280.dp, 800.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
    }

    // Phone-sized portraits (approx 411x891dp) for Home, Settings, and Alarms
    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_home_awake_mgdl_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val latest = GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.2, slopeName = "↗", source = "test")
            setUnits(Units.MG_DL)
            setMode(false)
            val hist = buildHistory(3, now, 142.0, "↗")
            renderAndSavePngActivity("phone_home_awake_mgdl.png") { Box(Modifier.size(411.dp, 891.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist) } }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_home_sleep_mgdl_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val latest = GlucoseReading(timestamp = now, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "Flat", source = "test")
            val hist = buildHistory(3, now, 120.0, "Flat")
            setUnits(Units.MG_DL)
            setMode(true)
            // Assert Settings button visibility and size before capturing
            composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist, previewIsSleep = true) } } }
            composeRule.waitUntil(5_000) {
                try {
                    composeRule.onNodeWithText("Settings").assertExists()
                    composeRule.onNodeWithTag("settingsButton").assertWidthIsAtLeast(48.dp)
                    true
                } catch (_: AssertionError) { false }
            }
            renderAndSavePngActivity("phone_home_sleep_mgdl.png") { Box(Modifier.size(411.dp, 891.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest, previewHistory = hist, previewIsSleep = true) } }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_settings_port() {
        // Assert narrow-layout toggles are on separate rows and safety label height reasonable
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { SettingsScreen(onBack = {}) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithTag("toggleRowVoice").assertExists()
                composeRule.onNodeWithTag("toggleRowSafety").assertExists()
                true
            } catch (_: AssertionError) { false }
        }
        renderAndSavePngActivity("phone_settings.png") { Box(Modifier.size(411.dp, 891.dp)) { SettingsScreen(onBack = {}) } }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_alarm_urgent_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val db = AppDatabase.get(context).glucoseDao()
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 65.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test"))
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -2.0, slopeName = "↘", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("URGENT ALARM").assertExists()
                composeRule.onNodeWithText("58 mg/dL", substring = true).assertExists()
                true
            }
            catch (_: AssertionError) { false }
        }
            composeRule.onNodeWithText("URGENT ALARM").assertExists()
            composeRule.onNodeWithText("58 mg/dL", substring = true).assertExists()
            composeRule.onNodeWithText("↘", substring = true).assertExists()
            composeRule.onNodeWithText("min ago", substring = true).assertExists()
        renderAndSavePngActivity("phone_alarm_urgent.png") {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_alarm_low_warn_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val db = AppDatabase.get(context).glucoseDao()
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 79.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test"))
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
            composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists()
            composeRule.onNodeWithText("↘", substring = true).assertExists()
            composeRule.onNodeWithText("min ago", substring = true).assertExists()
        renderAndSavePngActivity("phone_alarm_low_warn.png") {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW_WARN,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_alarm_low_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val db = AppDatabase.get(context).glucoseDao()
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 79.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test"))
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 76.0, slopeMgdlPerMin = -0.8, slopeName = "↘", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
            composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists()
            composeRule.onNodeWithText("↘", substring = true).assertExists()
            composeRule.onNodeWithText("min ago", substring = true).assertExists()
        renderAndSavePngActivity("phone_alarm_low.png") {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_LOW,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_alarm_high_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val db = AppDatabase.get(context).glucoseDao()
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 240.0, slopeMgdlPerMin = null, slopeName = "↗", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test"))
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 260.0, slopeMgdlPerMin = 1.8, slopeName = "↗", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("260 mg/dL", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
            composeRule.onNodeWithText("260 mg/dL", substring = true).assertExists()
            composeRule.onNodeWithText("↗", substring = true).assertExists()
            composeRule.onNodeWithText("min ago", substring = true).assertExists()
        renderAndSavePngActivity("phone_alarm_high.png") {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_alarm_signal_lost_port() {
        runBlocking {
            val now = System.currentTimeMillis()
            val db = AppDatabase.get(context).glucoseDao()
            db.insert(GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = null, slopeName = "→", source = "test"))
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 35 * 60_000L, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "→", source = "test")
        composeRule.setContent { G7Theme { Box(Modifier.size(411.dp, 891.dp)) { com.g7nightalarm.app.ui.alarm.AlarmContent(type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL, onDismiss = {}, animationsEnabled = false, previewLatest = latest, previewUnits = Units.MG_DL) } } }
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("120 mg/dL", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
            composeRule.onNodeWithText("120 mg/dL", substring = true).assertExists()
            composeRule.onNodeWithText("→", substring = true).assertExists()
            composeRule.onNodeWithText("min ago", substring = true).assertExists()
        renderAndSavePngActivity("phone_alarm_signal_lost.png") {
            Box(Modifier.size(411.dp, 891.dp)) {
                com.g7nightalarm.app.ui.alarm.AlarmContent(
                    type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_SIGNAL,
                    onDismiss = {},
                    animationsEnabled = false,
                    previewLatest = latest,
                    previewUnits = Units.MG_DL
                )
            }
        }
        }
    }
}

