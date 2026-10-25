package com.g7nightalarm.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.screens.HomeScreen
import com.g7nightalarm.app.ui.screens.SettingsScreen
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.runBlocking
import kotlin.math.sin

class ScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra("screen") ?: "home_awake_mgdl"
        when (screen) {
            "home_awake_mgdl" -> {
                seedReadings(24)
                setMode(false)
                setUnits(Units.MG_DL)
                setContent { renderHome(1280, 800) }
            }
            "home_sleep_mgdl" -> {
                seedReadings(24)
                setMode(true)
                setUnits(Units.MG_DL)
                setContent { renderHome(1280, 800) }
            }
            "home_awake_mmol_port" -> {
                seedReadings(3)
                setMode(false)
                setUnits(Units.MMOL_L)
                setContent { renderHome(800, 1280) }
            }
            "settings" -> {
                setContent { G7Theme { SettingsScreen(onBack = { finish() }) } }
            }
            else -> setContent { renderHome(1280, 800) }
        }
    }

    @Composable
    private fun renderHome(w: Int, h: Int) {
        G7Theme {
            Box(Modifier.size(w.dp, h.dp)) {
                HomeScreen(onOpenSettings = {})
            }
        }
    }

    private fun seedReadings(hours: Int) = runBlocking {
        val dao = AppDatabase.get(this@ScreenshotActivity).glucoseDao()
        val now = System.currentTimeMillis()
        val points = hours * 12
        for (i in 0 until points) {
            val t = now - (points - i) * 5L * 60L * 1000L
            val base = 120.0 + 20.0 * sin(i / 8.0)
            dao.insert(
                GlucoseReading(
                    timestamp = t,
                    mgdl = base,
                    slopeMgdlPerMin = null,
                    slopeName = "Flat",
                    source = "screenshot"
                )
            )
        }
    }

    private fun setUnits(units: Units) = runBlocking {
        val repo = SettingsRepository(this@ScreenshotActivity)
        repo.update { p -> p[SettingsKeys.units] = units.name }
    }

    private fun setMode(sleep: Boolean) = runBlocking {
        val repo = SettingsRepository(this@ScreenshotActivity)
        repo.update { p -> p[SettingsKeys.sleepMode] = sleep }
    }
}

