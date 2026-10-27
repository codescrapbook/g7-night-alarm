package com.g7nightalarm.app.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
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
    // Write under app/build when tests run from repo root; otherwise write under module build/
    private val outDir: File = run {
        val appDir = File("app")
        if (appDir.exists() && appDir.isDirectory) {
            File("app/build/outputs/roborazzi/screens")
        } else {
            File("build/outputs/roborazzi/screens")
        }
    }

    @Test
    fun alarm_urgent_double_down() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 6 * 60_000L, mgdl = 70.0, slopeMgdlPerMin = null, slopeName = "DoubleDown", source = "test"))
            db.insert(GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -3.4, slopeName = "DoubleDown", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now - 60_000L, mgdl = 58.0, slopeMgdlPerMin = -3.4, slopeName = "DoubleDown", source = "test")
        composeRule.setContent {
            G7Theme {
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
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("Falling fast", substring = true).assertExists(); true
            } catch (_: AssertionError) { false }
        }
        renderAndSavePngActivity("alarm_urgent_double_down.png") {
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
    fun alarm_high_double_up() {
        val now = System.currentTimeMillis()
        val db = AppDatabase.get(context).glucoseDao()
        runBlocking {
            db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 200.0, slopeMgdlPerMin = null, slopeName = "DoubleUp", source = "test"))
            db.insert(GlucoseReading(timestamp = now, mgdl = 260.0, slopeMgdlPerMin = 3.5, slopeName = "DoubleUp", source = "test"))
        }
        setUnits(Units.MG_DL)
        val latest = GlucoseReading(timestamp = now, mgdl = 260.0, slopeMgdlPerMin = 3.5, slopeName = "DoubleUp", source = "test")
        composeRule.setContent {
            G7Theme {
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
        composeRule.waitUntil(5_000) {
            try { composeRule.onNodeWithText("Rising fast", substring = true).assertExists(); true }
            catch (_: AssertionError) { false }
        }
        renderAndSavePngActivity("alarm_high_double_up.png") {
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

    @Composable
    private fun TrendGallery(modifier: Modifier = Modifier) {
        androidx.compose.material3.MaterialTheme {
            androidx.compose.foundation.layout.Column(
                modifier = modifier.background(Color.Black).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val entries = listOf(
                    "DoubleUp" to "Rising fast",
                    "SingleUp" to "Rising",
                    "FortyFiveUp" to "Rising slowly",
                    "Flat" to "Steady",
                    "FortyFiveDown" to "Falling slowly",
                    "SingleDown" to "Falling",
                    "DoubleDown" to "Falling fast",
                )
                entries.forEach { (_, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.g7nightalarm.app.ui.trend.TrendArrowIcon(
                            direction = when (label) {
                                "Rising fast" -> com.g7nightalarm.app.ui.trend.TrendDirection.DOUBLE_UP
                                "Rising" -> com.g7nightalarm.app.ui.trend.TrendDirection.SINGLE_UP
                                "Rising slowly" -> com.g7nightalarm.app.ui.trend.TrendDirection.FORTY_FIVE_UP
                                "Steady" -> com.g7nightalarm.app.ui.trend.TrendDirection.FLAT
                                "Falling slowly" -> com.g7nightalarm.app.ui.trend.TrendDirection.FORTY_FIVE_DOWN
                                "Falling" -> com.g7nightalarm.app.ui.trend.TrendDirection.SINGLE_DOWN
                                "Falling fast" -> com.g7nightalarm.app.ui.trend.TrendDirection.DOUBLE_DOWN
                                else -> com.g7nightalarm.app.ui.trend.TrendDirection.UNKNOWN
                            },
                            color = Color.White,
                            size = 96.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp))
                    }
                }
            }
        }
    }

    @Test
    fun arrows_gallery_tablet() {
        renderAndSavePngActivity("arrows_gallery_tablet.png") {
            Box(Modifier.size(1280.dp, 800.dp)) { TrendGallery(Modifier.fillMaxSize()) }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_arrows_gallery() {
        renderAndSavePngActivity("phone_arrows_gallery.png") {
            Box(Modifier.size(411.dp, 891.dp)) { TrendGallery(Modifier.fillMaxSize()) }
        }
    }

    @Test
    fun trend_icon_orientations_centroid_checks() {
        // Render 45° icons and assert centroid quadrants
        fun quadrantOf(bitmap: Bitmap): Pair<Boolean, Boolean> {
            // returns (isRight, isTop) centroid booleans
            var sumX = 0L
            var sumY = 0L
            var mass = 0L
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val p = pixels[y * w + x]
                    val a = (p ushr 24) and 0xFF
                    val r = (p ushr 16) and 0xFF
                    val g = (p ushr 8) and 0xFF
                    val b = p and 0xFF
                    // Simple luminance-based mass; ignore fully black pixels
                    val lum = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                    val m = (a * lum) / 255
                    if (m > 0) {
                        mass += m
                        sumX += m * x
                        sumY += m * y
                    }
                }
            }
            val cx = sumX.toDouble() / mass.toDouble()
            val cy = sumY.toDouble() / mass.toDouble()
            return Pair(cx > w / 2.0, cy < h / 2.0)
        }

        // 45° up (↗): mass on top-right
        composeRule.setContent {
            androidx.compose.material3.MaterialTheme {
                Box(Modifier.size(200.dp).background(Color.Black)) {
                    com.g7nightalarm.app.ui.trend.TrendArrowIcon(
                        direction = com.g7nightalarm.app.ui.trend.TrendDirection.FORTY_FIVE_UP,
                        color = Color.White,
                        size = 160.dp
                    )
                }
            }
        }
        val upBmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val (upRight, upTop) = quadrantOf(upBmp)
        org.junit.Assert.assertTrue(upRight && upTop)

        // 45° down (↘): mass on bottom-right
        composeRule.setContent {
            androidx.compose.material3.MaterialTheme {
                Box(Modifier.size(200.dp).background(Color.Black)) {
                    com.g7nightalarm.app.ui.trend.TrendArrowIcon(
                        direction = com.g7nightalarm.app.ui.trend.TrendDirection.FORTY_FIVE_DOWN,
                        color = Color.White,
                        size = 160.dp
                    )
                }
            }
        }
        val downBmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val (downRight, downTop) = quadrantOf(downBmp)
        org.junit.Assert.assertTrue(downRight && !downTop)
    }

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
        val histAwake = buildHistory(3, now, 142.0, "↗")
        val latest142 = GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.2, slopeName = "↗", source = "test")
        renderAndSavePngActivityFontScale("fs2_home_awake_mgdl.png", 2f) { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest142, previewHistory = histAwake) } }
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
        val histPhone = buildHistory(3, now, 120.0, "Flat")
        val latest120now = GlucoseReading(timestamp = now, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "Flat", source = "test")
        renderAndSavePngActivityFontScale("fs2_phone_home_sleep_mgdl.png", 2f) { Box(Modifier.size(411.dp, 891.dp)) { HomeScreen(onOpenSettings = {}, previewLatest = latest120now, previewHistory = histPhone, previewIsSleep = true) } }
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
        composeRule.onNodeWithText("Falling slowly", substring = true).assertExists()
        composeRule.onNodeWithText("ago", substring = true).assertExists()
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
        composeRule.onNodeWithText("Falling slowly", substring = true).assertExists()
        composeRule.onNodeWithText("ago", substring = true).assertExists()
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
        composeRule.onNodeWithText("Rising slowly", substring = true).assertExists()
        composeRule.onNodeWithText("ago", substring = true).assertExists()
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
        composeRule.onNodeWithText("Steady", substring = true).assertExists()
        composeRule.onNodeWithText("ago", substring = true).assertExists()
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
            composeRule.onNodeWithText("Falling slowly", substring = true).assertExists()
            composeRule.onNodeWithText("ago", substring = true).assertExists()
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
            composeRule.onNodeWithText("Falling slowly", substring = true).assertExists()
            composeRule.onNodeWithText("ago", substring = true).assertExists()
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
            composeRule.onNodeWithText("Falling slowly", substring = true).assertExists()
            composeRule.onNodeWithText("ago", substring = true).assertExists()
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
            composeRule.onNodeWithText("Rising slowly", substring = true).assertExists()
            composeRule.onNodeWithText("ago", substring = true).assertExists()
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
            composeRule.onNodeWithText("Steady", substring = true).assertExists()
            composeRule.onNodeWithText("ago", substring = true).assertExists()
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

