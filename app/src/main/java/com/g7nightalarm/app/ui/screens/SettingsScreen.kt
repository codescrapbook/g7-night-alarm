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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag

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
    var voiceEnabled by remember { mutableStateOf(state.voiceEnabled) }
    var safetyNetFull by remember { mutableStateOf(state.safetyNetAlwaysFull) }
    var autoHour by remember { mutableStateOf(state.autoSleepHour.toString()) }
    var autoMinute by remember { mutableStateOf(state.autoSleepMinute.toString()) }
    var awakeVol by remember { mutableStateOf(state.awakeVolumePercent.coerceIn(1, 100)) }
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val labelColor = MaterialTheme.colorScheme.onBackground
        val secondaryColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = labelColor)
        // Subtle purple stitch under the header for theme continuity
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(4.dp)
        ) {
            val w = size.width
            val stitchColor = androidx.compose.ui.graphics.Color(0xFF8E44AD)
            val stitchLen = 16f
            val gap = 10f
            var x = 0f
            while (x < w) {
                drawRect(
                    color = stitchColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(stitchLen, size.height)
                )
                x += stitchLen + gap
            }
        }
        SnoozeLengthControl(
            minutes = state.snoozeLengthMinutes,
            narrow = LocalConfiguration.current.screenWidthDp < 500,
            onSelect = { minutes ->
                scope.launch { repo.update { it[SettingsKeys.snoozeLengthMinutes] = minutes } }
            }
        )
        val tfColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
        val tfTextStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(urgentLow, onValueChange = { urgentLow = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Urgent low (mg/dL)") }, modifier = Modifier.weight(1f), colors = tfColors, textStyle = tfTextStyle, enabled = true)
            OutlinedTextField(low, onValueChange = { low = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Low (mg/dL)") }, modifier = Modifier.weight(1f), colors = tfColors, textStyle = tfTextStyle, enabled = true)
            OutlinedTextField(high, onValueChange = { high = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("High (mg/dL)") }, modifier = Modifier.weight(1f), colors = tfColors, textStyle = tfTextStyle, enabled = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(rapidFall, onValueChange = { rapidFall = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Rapid fall (mg/dL/min)") }, modifier = Modifier.weight(1f), colors = tfColors, textStyle = tfTextStyle, enabled = true)
            OutlinedTextField(noData, onValueChange = { noData = it.filter { ch -> ch.isDigit() } }, label = { Text("No data minutes") }, modifier = Modifier.weight(1f), colors = tfColors, textStyle = tfTextStyle, enabled = true)
        }
        val cfg = LocalConfiguration.current
        val isNarrow = cfg.screenWidthDp < 500
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggleRowVoice"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Voice control (\"good night/morning\")",
                    color = labelColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = if (isNarrow) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(checked = voiceEnabled, onCheckedChange = { voiceEnabled = it })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggleRowSafety"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Safety-net urgent/no-data 30m = full alarm",
                    color = labelColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("safetyLabel"),
                    maxLines = if (isNarrow) 3 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(checked = safetyNetFull, onCheckedChange = { safetyNetFull = it })
            }
        }
        // Music Mode section
        Text("Music Mode (awake only)", style = MaterialTheme.typography.titleLarge, color = labelColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enable Music Mode", color = labelColor)
            Switch(checked = musicMode, onCheckedChange = {
                musicMode = it
                scope.launch { repo.update { p -> p[SettingsKeys.musicModeEnabled] = it } }
            })
        }
        // Low song picker (default melody if none)
        if (isNarrow) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                Text("Low song", color = labelColor, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                val desc = if (musicLowUri.isNotBlank()) "Custom audio selected" else "Default melody: Swing Low, Sweet Chariot"
                Text(desc, color = secondaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { pickAudioLowLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioLowLauncher != null, modifier = Modifier.weight(1f)) { Text("Pick") }
                    Button(onClick = {
                        scope.launch { repo.update { it.remove(SettingsKeys.musicLowUri) } }
                        musicLowUri = ""
                    }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    Button(onClick = {
                        val vol = awakeVol.coerceIn(1, 100)
                        scope.launch {
                            if (musicLowUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicLowUri))
                            else alarm.startAwakeMusicLowDefault(vol)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Preview") }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Low song", color = labelColor)
                    val desc = if (musicLowUri.isNotBlank()) "Custom audio selected" else "Default melody: Swing Low, Sweet Chariot"
                    Text(desc, color = secondaryColor)
                }
                Button(onClick = { pickAudioLowLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioLowLauncher != null) { Text("Pick") }
                Button(onClick = {
                    scope.launch { repo.update { it.remove(SettingsKeys.musicLowUri) } }
                    musicLowUri = ""
                }) { Text("Clear") }
                Button(onClick = {
                    val vol = awakeVol.coerceIn(1, 100)
                    scope.launch {
                        if (musicLowUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicLowUri))
                        else alarm.startAwakeMusicLowDefault(vol)
                    }
                }) { Text("Preview") }
            }
        }
        // High song picker
        if (isNarrow) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                Text("High song", color = labelColor, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                val desc = if (musicHighUri.isNotBlank()) "Custom audio selected" else "Default loop: Going Up"
                Text(desc, color = secondaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { pickAudioHighLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioHighLauncher != null, modifier = Modifier.weight(1f)) { Text("Pick") }
                    Button(onClick = {
                        scope.launch { repo.update { it.remove(SettingsKeys.musicHighUri) } }
                        musicHighUri = ""
                    }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    Button(onClick = {
                        val vol = awakeVol.coerceIn(1, 100)
                        scope.launch {
                            if (musicHighUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicHighUri))
                            else alarm.startAwakeMusicHighDefault(vol)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Preview") }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("High song", color = labelColor)
                    val desc = if (musicHighUri.isNotBlank()) "Custom audio selected" else "Default loop: Going Up"
                    Text(desc, color = secondaryColor)
                }
                Button(onClick = { pickAudioHighLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioHighLauncher != null) { Text("Pick") }
                Button(onClick = {
                    scope.launch { repo.update { it.remove(SettingsKeys.musicHighUri) } }
                    musicHighUri = ""
                }) { Text("Clear") }
                Button(onClick = {
                    val vol = awakeVol.coerceIn(1, 100)
                    scope.launch {
                        if (musicHighUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicHighUri))
                        else alarm.startAwakeMusicHighDefault(vol)
                    }
                }) { Text("Preview") }
            }
        }
        // Signal lost song picker
        if (isNarrow) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                Text("Signal lost song", color = labelColor, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                val desc = if (musicSignalUri.isNotBlank()) "Custom audio selected" else "Default loop: Lost Connection"
                Text(desc, color = secondaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { pickAudioSignalLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioSignalLauncher != null, modifier = Modifier.weight(1f)) { Text("Pick") }
                    Button(onClick = {
                        scope.launch { repo.update { it.remove(SettingsKeys.musicSignalUri) } }
                        musicSignalUri = ""
                    }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    Button(onClick = {
                        val vol = awakeVol.coerceIn(1, 100)
                        scope.launch {
                            if (musicSignalUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicSignalUri))
                            else alarm.startAwakeMusicSignalDefault(vol)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Preview") }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Signal lost song", color = labelColor)
                    val desc = if (musicSignalUri.isNotBlank()) "Custom audio selected" else "Default loop: Lost Connection"
                    Text(desc, color = secondaryColor)
                }
                Button(onClick = { pickAudioSignalLauncher?.launch(arrayOf("audio/*")) }, enabled = pickAudioSignalLauncher != null) { Text("Pick") }
                Button(onClick = {
                    scope.launch { repo.update { it.remove(SettingsKeys.musicSignalUri) } }
                    musicSignalUri = ""
                }) { Text("Clear") }
                Button(onClick = {
                    val vol = awakeVol.coerceIn(1, 100)
                    scope.launch {
                        if (musicSignalUri.isNotBlank()) alarm.startAwakeMusicFromUri(vol, Uri.parse(musicSignalUri))
                        else alarm.startAwakeMusicSignalDefault(vol)
                    }
                }) { Text("Preview") }
            }
        }
        Text("Awake volume: $awakeVol%", style = MaterialTheme.typography.titleMedium, color = labelColor)
        Slider(value = awakeVol.toFloat(), onValueChange = { v -> awakeVol = v.toInt().coerceIn(1, 100) }, valueRange = 1f..100f)
        Button(onClick = {
            val am = com.g7nightalarm.app.alarm.AlarmController(context)
            scope.launch { am.previewAwakeChime(awakeVol) }
        }) { Text("Preview volume") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Units: ${if (state.units == com.g7nightalarm.app.settings.Units.MG_DL) "mg/dL" else "mmol/L"}", color = labelColor)
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
                        p[SettingsKeys.rapidFall] = rapidFall.toDoubleOrNull() ?: 3.0
                        p[SettingsKeys.noDataMin] = noData.toIntOrNull() ?: 30
                        p[SettingsKeys.voiceEnabled] = voiceEnabled
                        p[SettingsKeys.safetyNetAlwaysFull] = safetyNetFull
                        p[SettingsKeys.autoSleepHour] = autoHour.toIntOrNull()?.coerceIn(0, 23) ?: 23
                        p[SettingsKeys.autoSleepMinute] = autoMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        p[SettingsKeys.awakeVolumePercent] = awakeVol.coerceIn(1, 100)
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
        Text("Permissions & setup", color = labelColor)
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
        Text("Offline voice: enabling Voice control downloads the small English model automatically.", color = secondaryColor)
        Button(onClick = { alarm.startAlarm() }) { Text("Test alarm") }
    }
}

@Composable
private fun SnoozeLengthControl(
    minutes: Int,
    narrow: Boolean,
    onSelect: (Int) -> Unit
) {
    val labelColor = MaterialTheme.colorScheme.onBackground
    Text(
        "Snooze length",
        color = labelColor,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        modifier = Modifier.testTag("snoozeLengthLabel")
    )
    Text(
        "Wake you again after",
        color = labelColor,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    )
    val choices = com.g7nightalarm.app.alarm.SnoozePolicy.lengthChoices
    if (narrow) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SnoozeLengthButton(choices[0], minutes, Modifier.weight(1f), onSelect)
                SnoozeLengthButton(choices[1], minutes, Modifier.weight(1f), onSelect)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SnoozeLengthButton(choices[2], minutes, Modifier.weight(1f), onSelect)
                SnoozeLengthButton(choices[3], minutes, Modifier.weight(1f), onSelect)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { choice ->
                SnoozeLengthButton(choice, minutes, Modifier.weight(1f), onSelect)
            }
        }
    }
}

@Composable
private fun SnoozeLengthButton(
    choice: Int,
    selectedMinutes: Int,
    modifier: Modifier,
    onSelect: (Int) -> Unit
) {
    val selected = choice == selectedMinutes
    val bg = if (selected) Color.White else Color(0xFF1B1B1B)
    val fg = if (selected) Color.Black else Color.White
    Button(
        onClick = { onSelect(choice) },
        modifier = modifier
            .heightIn(min = 72.dp)
            .testTag("snoozeLength$choice"),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        border = BorderStroke(3.dp, Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            "$choice min",
            color = fg,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            maxLines = 1
        )
    }
}

