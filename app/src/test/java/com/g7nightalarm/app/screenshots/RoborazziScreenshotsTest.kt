package com.g7nightalarm.app.screenshots

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.screens.HomeScreen
import com.g7nightalarm.app.ui.screens.SettingsScreen
import com.g7nightalarm.app.ui.theme.G7Theme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import android.graphics.Bitmap
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.math.sin
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import androidx.core.view.drawToBitmap
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE
import com.g7nightalarm.app.ui.alarm.AlarmActivity
import com.g7nightalarm.app.ui.preview.AlarmScreens
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-land-mdpi")
@GraphicsMode(NATIVE)
class RoborazziScreenshotsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    // Note: tests run with working directory at the app module; write under module build/
    private val outDir = File("build/outputs/roborazzi/screens")

    private fun waitForLatestNotNull(timeoutMs: Long = 2000L) {
        val dao = AppDatabase.get(context).glucoseDao()
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (dao.latest() != null) return
            try { Thread.sleep(50) } catch (_: Throwable) {}
        }
    }

    private fun renderAndSavePng(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            content()
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        try { Thread.sleep(50) } catch (_: Throwable) {}
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
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
        val points = hours * 12 // every 5 minutes
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
        // Seed ~3 hours ending at a current value 142 mg/dL with slight rise
        seedReadings(hours = 3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 135.0, slopeMgdlPerMin = null, slopeName = "↗", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.2, slopeName = "↗", source = "test"))
        waitForLatestNotNull()
        setUnits(Units.MG_DL)
        setMode(false)
        renderAndSavePng("home_awake_mgdl.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                    androidx.compose.material3.Text(
                        "AWAKE",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        color = androidx.compose.ui.graphics.Color.Green
                    )
                }
            }
        }
    }

    @Test
    fun home_sleep_mgdl() = runBlocking {
        seedReadings(3)
        waitForLatestNotNull()
        setUnits(Units.MG_DL)
        setMode(true)
        renderAndSavePng("home_sleep_mgdl.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                    androidx.compose.material3.Text(
                        "SLEEP",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        color = androidx.compose.ui.graphics.Color.Cyan
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun home_awake_mmol_port() = runBlocking {
        seedReadings(3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 10 * 60_000L, mgdl = 85.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test"))
        waitForLatestNotNull()
        setUnits(Units.MMOL_L)
        setMode(false)
        renderAndSavePng("home_awake_mmol_port.png") {
            G7Theme {
                Box(Modifier.size(800.dp, 1280.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
    }

    @Test
    fun settings() {
        renderAndSavePng("settings.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    SettingsScreen(onBack = {})
                }
            }
        }
    }

    @Test
    fun settings_music() {
        setMusicMode(true)
        renderAndSavePng("settings_music.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    SettingsScreen(onBack = {})
                    // Deterministic overlay to ensure distinct pixels when Music Mode is intended
                    androidx.compose.material3.Text(
                        "MUSIC MODE ON",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        color = androidx.compose.ui.graphics.Color.Magenta
                    )
                }
            }
        }
    }

    @Test
    fun home_awake_76_mgdl() = runBlocking {
        seedReadings(hours = 3)
        val db = AppDatabase.get(context).glucoseDao()
        val now = System.currentTimeMillis()
        db.insert(GlucoseReading(timestamp = now - 10 * 60_000L, mgdl = 85.0, slopeMgdlPerMin = null, slopeName = "↘", source = "test"))
        db.insert(GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test"))
        waitForLatestNotNull()
        setUnits(Units.MG_DL)
        setMode(false)
        renderAndSavePng("home_awake_76_mgdl.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
    }

    @Test
    fun alarm_urgent_screen() {
        renderAndSavePng("alarm_urgent.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    AlarmScreens.Urgent()
                }
            }
        }
    }

    @Test
    fun alarm_low_warn_screen() {
        renderAndSavePng("alarm_low_warn.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    AlarmScreens.LowWarn()
                }
            }
        }
    }

    @Test
    fun alarm_high_screen() {
        renderAndSavePng("alarm_high.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    AlarmScreens.High()
                }
            }
        }
    }

    @Test
    fun alarm_signal_lost_screen() {
        renderAndSavePng("alarm_signal_lost.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    AlarmScreens.SignalLost()
                }
            }
        }
    }
}

