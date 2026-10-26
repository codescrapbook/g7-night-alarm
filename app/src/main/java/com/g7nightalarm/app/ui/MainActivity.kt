package com.g7nightalarm.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.g7nightalarm.app.service.MonitorService
import com.g7nightalarm.app.ui.screens.HomeScreen
import com.g7nightalarm.app.ui.screens.SettingsScreen
import com.g7nightalarm.app.ui.theme.G7Theme
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MonitorService.start(this)
        // Real first-run prompt for voice mic on release builds only (avoid test flakiness)
        if (!BuildConfig.DEBUG) {
            val recordPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
            lifecycleScope.launch {
                try {
                    val voiceOn = SettingsRepository(this@MainActivity).flow.first().voiceEnabled
                    val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (voiceOn && !granted) {
                        recordPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } catch (_: Throwable) { }
            }
        }
        val startOnSettings = intent?.getBooleanExtra("debug_open_settings", false) == true
        setContent {
            G7Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav(startOnSettings = startOnSettings)
                }
            }
        }
    }
}

@Composable
private fun AppNav(startOnSettings: Boolean) {
    val nav = rememberNavController()
    val start = if (startOnSettings) "settings" else "home"
    NavHost(navController = nav, startDestination = start) {
        composable("home") { HomeScreen(onOpenSettings = { nav.navigate("settings") }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}

