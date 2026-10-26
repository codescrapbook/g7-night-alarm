package com.g7nightalarm.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val state by repo.flow.collectAsState(initial = com.g7nightalarm.app.settings.SettingsState())
    var urgentLow by remember { mutableStateOf(state.thresholds.urgentLow.toString()) }
    var low by remember { mutableStateOf(state.thresholds.low.toString()) }
    var high by remember { mutableStateOf(state.thresholds.high.toString()) }
    var rapidFall by remember { mutableStateOf(state.thresholds.rapidFallRateMgdlPerMin.toString()) }
    var noData by remember { mutableStateOf(state.thresholds.noDataMinutes.toString()) }
    var allowSnooze by remember { mutableStateOf(state.allowAlarmSnooze) }
    var voiceEnabled by remember { mutableStateOf(state.voiceEnabled) }
    var safetyNetFull by remember { mutableStateOf(state.safetyNetAlwaysFull) }
    var autoHour by remember { mutableStateOf(state.autoSleepHour.toString()) }
    var autoMinute by remember { mutableStateOf(state.autoSleepMinute.toString()) }
    var awakeVolPct by remember { mutableStateOf(state.awakeVolumePercent.coerceIn(1, 100)) }
    var musicMode by remember { mutableStateOf(state.musicModeEnabled) }
    var musicLowUri by remember { mutableStateOf(state.musicLowUri ?: "") }
    var musicHighUri by remember { mutableStateOf(state.musicHighUri ?: "") }
    var musicSignalUri by remember { mutableStateOf(state.musicSignalUri ?: "") }
    val scope = remember { CoroutineScope(Dispatchers.IO) }
    val alarm = remember { AlarmController(context) }
    // SAF pickers (only if an Activity is present; Robolectric compose tests may lack one)
    val activity = context as? Activity
    val pickAudioLowLauncher = if (activity != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {}
                musicLowUri = uri.toString()
                scope.launch { repo.update { it[SettingsKeys.musicLowUri] = musicLowUri } }
            }
        }
    } else null
    val pickAudioHighLauncher = if (activity != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {}
                musicHighUri = uri.toString()
                scope.launch { repo.update { it[SettingsKeys.musicHighUri] = musicHighUri } }
            }
        }
    } else null
    val pickAudioSignalLauncher = if (activity != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {}
                musicSignalUri = uri.toString()
                scope.launch { repo.update { it[SettingsKeys.musicSignalUri] = musicSignalUri } }
            }
        }
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(urgentLow, onValueChange = { urgentLow = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Urgent low (mg/dL)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(low, onValueChange = { low = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Low (mg/dL)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(high, onValueChange = { high = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("High (mg/dL)") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(rapidFall, onValueChange = { rapidFall = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Rapid fall (mg/dL/min)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(noData, onValueChange = { noData = it.filter { ch -> ch.isDigit() } }, label = { Text("No data minutes (signal lost)") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Allow snooze")
                Switch(checked = allowSnooze, onCheckedChange = { allowSnooze = it })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Voice control (\"good night/morning\")")
                Switch(checked = voiceEnabled, onCheckedChange = { voiceEnabled = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Safety-net urgent/no-data 30m = full alarm")
                Switch(checked = safetyNetFull, onCheckedChange = { safetyNetFull = it })
            }
        }
        // Music Mode section
        Text("Music Mode (awake only)", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enable Music Mode")
            Switch(checked = musicMode, onCheckedChange = {
                musicMode = it
                scope.launch { repo.update { p -> p[SettingsKeys.musicModeEnabled] = it } }
            })
        }
        // Low song picker (default melody if none)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Low song")
                val desc = if (musicLowUri.isNotBlank()) "Custom audio selected" else "Default melody: Swing Low, Sweet Chariot"
                Text(desc)
            }
            Button(onClick = { pickAudioLowLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioLowLauncher != null) { Text("Pick") }
            Button(onClick = {
                scope.launch { repo.update { it.remove(SettingsKeys.musicLowUri) } }
                musicLowUri = ""
            }) { Text("Clear") }
            Button(onClick = {
                val vol = awakeVolPct.coerceIn(1, 100)
                scope.launch {
                    if (musicLowUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicLowUri))
                    else alarm.startAwakeMusicLowDefault(vol)
                }
            }) { Text("Preview") }
        }
        // High song picker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("High song")
                val desc = if (musicHighUri.isNotBlank()) "Custom audio selected" else "Default loop: Going Up"
                Text(desc)
            }
            Button(onClick = { pickAudioHighLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioHighLauncher != null) { Text("Pick") }
            Button(onClick = {
                scope.launch { repo.update { it.remove(SettingsKeys.musicHighUri) } }
                musicHighUri = ""
            }) { Text("Clear") }
            Button(onClick = {
                val vol = awakeVolPct.coerceIn(1, 100)
                scope.launch {
                    if (musicHighUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicHighUri))
                    else alarm.startAwakeMusicHighDefault(vol)
                }
            }) { Text("Preview") }
        }
        // Signal lost song picker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Signal lost song")
                val desc = if (musicSignalUri.isNotBlank()) "Custom audio selected" else "Default loop: Lost Connection"
                Text(desc)
            }
            Button(onClick = { pickAudioSignalLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioSignalLauncher != null) { Text("Pick") }
            Button(onClick = {
                scope.launch { repo.update { it.remove(SettingsKeys.musicSignalUri) } }
                musicSignalUri = ""
            }) { Text("Clear") }
            Button(onClick = {
                val vol = awakeVolPct.coerceIn(1, 100)
                scope.launch {
                    if (musicSignalUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicSignalUri))
                    else alarm.startAwakeMusicSignalDefault(vol)
                }
            }) { Text("Preview") }
        }
        Text("Awake volume: ${awakeVolPct}%")
        Slider(
            value = awakeVolPct.toFloat(),
            onValueChange = { v -> awakeVolPct = v.toInt().coerceIn(1, 100) },
            valueRange = 1f..100f,
            onValueChangeFinished = {
                val vol = awakeVolPct.coerceIn(1, 100)
                scope.launch {
                    repo.update { it[SettingsKeys.awakeVolumePercent] = vol }
                    alarm.previewAwakeChime(vol)
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Units: ${if (state.units == com.g7nightalarm.app.settings.Units.MG_DL) "mg/dL" else "mmol/L"}")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val next = if (state.units == com.g7nightalarm.app.settings.Units.MG_DL)
                        com.g7nightalarm.app.settings.Units.MMOL_L else com.g7nightalarm.app.settings.Units.MG_DL
                    scope.launch { repo.update { it[SettingsKeys.units] = next.name } }
                }) { Text("Toggle Units") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(autoHour, onValueChange = { autoHour = it.filter { ch -> ch.isDigit() }.take(2) }, label = { Text("Auto Sleep Hour (0-23)") })
            OutlinedTextField(autoMinute, onValueChange = { autoMinute = it.filter { ch -> ch.isDigit() }.take(2) }, label = { Text("Minute (0-59)") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                scope.launch {
                    repo.update { p ->
                        p[SettingsKeys.urgentLow] = urgentLow.toDoubleOrNull() ?: 70.0
                        p[SettingsKeys.low] = low.toDoubleOrNull() ?: 80.0
                        p[SettingsKeys.high] = high.toDoubleOrNull() ?: 250.0
                        p[SettingsKeys.rapidFall] = rapidFall.toDoubleOrNull() ?: 2.0
                        p[SettingsKeys.noDataMin] = noData.toIntOrNull() ?: 30
                        p[SettingsKeys.allowSnooze] = allowSnooze
                        p[SettingsKeys.voiceEnabled] = voiceEnabled
                        p[SettingsKeys.safetyNetAlwaysFull] = safetyNetFull
                        p[SettingsKeys.autoSleepHour] = autoHour.toIntOrNull()?.coerceIn(0, 23) ?: 23
                        p[SettingsKeys.autoSleepMinute] = autoMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        p[SettingsKeys.awakeVolumePercent] = awakeVolPct.coerceIn(1, 100)
                        p[SettingsKeys.musicModeEnabled] = musicMode
                        if (musicLowUri.isNotBlank()) p[SettingsKeys.musicLowUri] = musicLowUri else p.remove(SettingsKeys.musicLowUri)
                        if (musicHighUri.isNotBlank()) p[SettingsKeys.musicHighUri] = musicHighUri else p.remove(SettingsKeys.musicHighUri)
                        if (musicSignalUri.isNotBlank()) p[SettingsKeys.musicSignalUri] = musicSignalUri else p.remove(SettingsKeys.musicSignalUri)
                    }
                }
            }) { Text("Save") }
            Button(onClick = { onBack() }) { Text("Back") }
        }
        Spacer(Modifier.weight(1f))
        Text("Permissions & setup")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } else {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }) { Text("Notifications") }
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Battery optimization") }
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Do Not Disturb access") }
        }
        Text("Offline voice: enabling Voice control downloads the small English model automatically.")
        Button(onClick = { alarm.startAlarm() }) { Text("Test alarm") }
    }
}

