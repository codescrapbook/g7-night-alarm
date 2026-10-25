package com.g7nightalarm.app.screenshots

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
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
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.sin
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoborazziScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File("app/build/outputs/roborazzi/screens")

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
    fun home_awake_mgdl_24h_land() = runBlocking {
        seedReadings(24)
        setUnits(Units.MG_DL)
        setMode(false)
        outDir.mkdirs()
        composeRule.setContent {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
        savePng("home_awake_mgdl_24h_land.png")
    }

    @Test
    fun home_sleep_mgdl_24h_land() = runBlocking {
        seedReadings(24)
        setUnits(Units.MG_DL)
        setMode(true)
        outDir.mkdirs()
        composeRule.setContent {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
        savePng("home_sleep_mgdl_24h_land.png")
    }

    @Test
    fun home_awake_mmol_3h_port() = runBlocking {
        seedReadings(3)
        setUnits(Units.MMOL_L)
        setMode(false)
        outDir.mkdirs()
        composeRule.setContent {
            G7Theme {
                Box(Modifier.size(800.dp, 1280.dp)) {
                    HomeScreen(onOpenSettings = {})
                }
            }
        }
        savePng("home_awake_mmol_3h_port.png")
    }

    @Test
    fun settings_land() {
        outDir.mkdirs()
        composeRule.setContent {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    SettingsScreen(onBack = {})
                }
            }
        }
        savePng("settings_land.png")
    }

    @Test
    fun settings_music_land() {
        setMusicMode(true)
        outDir.mkdirs()
        composeRule.setContent {
            G7Theme {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    SettingsScreen(onBack = {})
                }
            }
        }
        savePng("settings_music_land.png")
    }

    private fun savePng(name: String) {
        // Ensure composition settled
        composeRule.waitForIdle()
        val img = composeRule.onRoot().captureToImage()
        val bmp = img.asAndroidBitmap()
        outDir.mkdirs()
        FileOutputStream(File(outDir, name)).use { fos ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fos)
        }
    }
}

