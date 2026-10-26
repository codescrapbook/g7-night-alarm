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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ApplicationProvider
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
        list.add(GlucoseReading(timestamp = endNow - 5 * 60_000L, mgdl = (endMgdl - 7.0), slopeMgdlPerMin = null, slopeName = slopeName, source = "test"))
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
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.Urgent() } } }
        composeRule.onNodeWithText("HOLD").assertExists()
        renderAndSavePngActivity("alarm_urgent.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.Urgent() } }
    }

    @Test
    fun alarm_low_warn_screen() {
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.LowWarn() } } }
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_low_warn.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.LowWarn() } }
    }

    @Test
    fun alarm_high_screen() {
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.High() } } }
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_high.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.High() } }
    }

    @Test
    fun alarm_signal_lost_screen() {
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.SignalLost() } } }
        composeRule.onNodeWithText("DISMISS").assertExists()
        renderAndSavePngActivity("alarm_signal_lost.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.SignalLost() } }
    }
}

