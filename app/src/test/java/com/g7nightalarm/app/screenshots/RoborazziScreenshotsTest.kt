package com.g7nightalarm.app.screenshots

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File("build/outputs/roborazzi/screens")

    private fun renderAndSavePng(name: String, content: @Composable () -> Unit) {
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
        seedReadings(3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 135.0, slopeMgdlPerMin = null, slopeName = "↗", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.2, slopeName = "↗", source = "test"))
        setUnits(Units.MG_DL)
        setMode(false)
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("142 mg/dL", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        renderAndSavePng("home_awake_mgdl.png") { HomeScreen(onOpenSettings = {}) }
    }

    @Test
    fun home_sleep_mgdl() = runBlocking {
        seedReadings(3)
        setUnits(Units.MG_DL)
        setMode(true)
        renderAndSavePng("home_sleep_mgdl.png") { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}) } }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun home_awake_mmol_port() = runBlocking {
        seedReadings(3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 10 * 60_000L, mgdl = 85.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test"))
        setUnits(Units.MMOL_L)
        setMode(false)
        renderAndSavePng("home_awake_mmol_port.png") { Box(Modifier.size(800.dp, 1280.dp)) { HomeScreen(onOpenSettings = {}) } }
    }

    @Test
    fun settings() {
        renderAndSavePng("settings.png") { Box(Modifier.size(1280.dp, 800.dp)) { SettingsScreen(onBack = {}) } }
    }

    @Test
    fun settings_music() {
        setMusicMode(true)
        renderAndSavePng("settings_music.png") { Box(Modifier.size(1280.dp, 800.dp)) { SettingsScreen(onBack = {}) } }
    }

    @Test
    fun home_awake_76_mgdl() = runBlocking {
        seedReadings(3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 10 * 60_000L, mgdl = 85.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test"))
        setUnits(Units.MG_DL)
        setMode(false)
        composeRule.setContent { G7Theme { Box(Modifier.size(1280.dp, 800.dp)) { HomeScreen(onOpenSettings = {}) } } }
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("76 mg/dL", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        renderAndSavePng("home_awake_76_mgdl.png") { HomeScreen(onOpenSettings = {}) }
    }

    @Test
    fun alarm_urgent_screen() {
        renderAndSavePng("alarm_urgent.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.Urgent() } }
    }

    @Test
    fun alarm_low_warn_screen() {
        renderAndSavePng("alarm_low_warn.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.LowWarn() } }
    }

    @Test
    fun alarm_high_screen() {
        renderAndSavePng("alarm_high.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.High() } }
    }

    @Test
    fun alarm_signal_lost_screen() {
        renderAndSavePng("alarm_signal_lost.png") { Box(Modifier.size(1280.dp, 800.dp)) { AlarmScreens.SignalLost() } }
    }
}

