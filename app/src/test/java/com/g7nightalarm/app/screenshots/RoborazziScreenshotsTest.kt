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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(NATIVE)
class RoborazziScreenshotsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File("app/build/outputs/roborazzi/screens")

    private fun renderAndSavePng(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            content()
        }
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
        seedReadings(24)
        setUnits(Units.MG_DL)
        setMode(false)
        renderAndSavePng("home_awake_mgdl.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
    }

    @Test
    fun home_sleep_mgdl() = runBlocking {
        seedReadings(24)
        setUnits(Units.MG_DL)
        setMode(true)
        renderAndSavePng("home_sleep_mgdl.png") {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
    }

    @Test
    fun home_awake_mmol_port() = runBlocking {
        seedReadings(3)
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
                }
            }
        }
    }

    @Test
    fun alarm_urgent_activity() {
        val activity = Robolectric.buildActivity(AlarmActivity::class.java).setup().get()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        outDir.mkdirs()
        val file = File(outDir, "alarm_urgent.png")
        val bitmap = activity.window.decorView.drawToBitmap()
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
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

