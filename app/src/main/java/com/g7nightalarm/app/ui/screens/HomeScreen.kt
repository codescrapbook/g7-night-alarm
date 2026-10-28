package com.g7nightalarm.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import com.g7nightalarm.app.ui.trend.InlineTrendReading
import com.g7nightalarm.app.ui.trend.deriveTrend
import com.g7nightalarm.app.ui.trend.TrendDirection
import com.g7nightalarm.app.ui.trend.label
import com.g7nightalarm.app.ui.trend.contentDescription
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.util.UnitsUtil
import android.os.Build
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    previewLatest: com.g7nightalarm.app.data.GlucoseReading? = null,
    previewHistory: List<com.g7nightalarm.app.data.GlucoseReading>? = null,
    previewIsSleep: Boolean? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember { AppDatabase.get(context).glucoseDao() }
    val settingsRepo = remember { SettingsRepository(context) }
    val modeRepo = remember { ModeRepository(context) }
    val latest by if (previewLatest != null) {
        remember { mutableStateOf(previewLatest) }
    } else {
        remember { dao.latestFlow() }.collectAsState(initial = null)
    }
    var hours by remember { mutableIntStateOf(24) }
    val settings by settingsRepo.flow.collectAsState(initial = com.g7nightalarm.app.settings.SettingsState())
    val isSleep = previewIsSleep ?: settings.sleepMode
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO) }
    val sinceMs = System.currentTimeMillis() - hours * 60L * 60L * 1000L
    val history by if (previewHistory != null) {
        remember(previewHistory) { mutableStateOf(previewHistory) }
    } else {
        remember(hours) { dao.streamSince(sinceMs).map { it } }.collectAsState(initial = emptyList())
    }

    // Reading header (value + units); trend arrow and label rendered separately
    val title = if (latest == null) {
        "— ${if (settings.units == Units.MG_DL) "mg/dL" else "mmol/L"}"
    } else {
        "${UnitsUtil.formatValue(latest!!.mgdl, settings.units)} ${if (settings.units == Units.MG_DL) "mg/dL" else "mmol/L"}"
    }
    val ageMin = latest?.let { ((System.currentTimeMillis() - it.timestamp) / 60000.0).toInt() } ?: 0
    val trend = deriveTrend(latest, previous = null)
    val spokenReading = if (latest == null) {
        "No recent reading"
    } else {
        val unitWords = if (settings.units == Units.MG_DL) "milligrams per deciliter" else "millimoles per liter"
        val base = "${UnitsUtil.formatValue(latest!!.mgdl, settings.units)} $unitWords"
        if (trend.direction == TrendDirection.UNKNOWN) base
        else "$base, ${trend.contentDescription().replaceFirstChar { it.lowercase() }}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
    ) {
        // On Android 14+, warn the user if full-screen intent is not allowed for this app
        val nm = remember { androidx.core.content.ContextCompat.getSystemService(context, NotificationManager::class.java) }
        val canFullScreen = if (Build.VERSION.SDK_INT >= 34) {
            try { nm?.canUseFullScreenIntent() == true } catch (_: Throwable) { false }
        } else true
        // Note: Removed decorative border to keep production and screenshots clean and high-contrast
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        if (!canFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF59D), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Full-screen alarms are disabled. Enable to allow urgent alarms to appear over lock screen.",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    try {
                        val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (_: Throwable) { }
                }) {
                    Text("Enable")
                }
            }
        }
        Text(
            "Undead In Bed",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.headlineLarge
        )
        val cfg = LocalConfiguration.current
        val isNarrow = cfg.screenWidthDp < 500
        if (isNarrow) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    InlineTrendReading(
                        reading = title,
                        direction = trend.direction,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        contentDescription = spokenReading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        trend.label(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Updated ${ageMin}m ago · Mode: ${if (isSleep) "Sleep" else "Awake"}",
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                    )
                }
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("settingsButton"),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Settings") }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    // High-contrast, large digits readable at distance
                    InlineTrendReading(
                        reading = title,
                        direction = trend.direction,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        contentDescription = spokenReading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        trend.label(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Updated ${ageMin}m ago · Mode: ${if (isSleep) "Sleep" else "Awake"}",
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                    )
                }
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("settingsButton"),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Settings") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                scope.launch {
                    modeRepo.setMode(AppMode.SLEEP, ModeTrigger.BUTTON)
                    settingsRepo.update { it[SettingsKeys.sleepMode] = true }
                }
            },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Good Night") }
            Button(
                onClick = {
                scope.launch {
                    modeRepo.setMode(AppMode.AWAKE, ModeTrigger.BUTTON)
                    settingsRepo.update { it[SettingsKeys.sleepMode] = false }
                }
            },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Good Morning") }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Color(0xFF181818), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            GlucoseChart(
                history = computeChartSeries(history, latest),
                low = UnitsUtil.convertThreshold(80.0, settings.units),
                urgentLow = UnitsUtil.convertThreshold(70.0, settings.units),
                high = UnitsUtil.convertThreshold(250.0, settings.units),
                units = settings.units
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(3, 6, 12, 24).forEach { h ->
                Button(
                    onClick = { hours = h },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("${h}h") }
            }
        }
        Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlucoseChart(
    history: List<com.g7nightalarm.app.data.GlucoseReading>,
    low: Double,
    urgentLow: Double,
    high: Double,
    units: Units
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Draw threshold bands
        fun yFor(v: Double): Float {
            val minV = if (units == Units.MG_DL) 40.0 else 40.0 / 18.0
            val maxV = if (units == Units.MG_DL) 300.0 else 300.0 / 18.0
            val clamped = v.coerceIn(minV, maxV)
            val norm = (clamped - minV) / (maxV - minV)
            return (h - (norm * h)).toFloat()
        }
        drawRect(color = Color(0x22FF1744), topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, yFor(high).coerceAtLeast(0f)))
        val lowBandTop = yFor(low)
        val urgentBandTop = yFor(urgentLow)
        drawRect(color = Color(0x222196F3), topLeft = androidx.compose.ui.geometry.Offset(0f, lowBandTop), size = androidx.compose.ui.geometry.Size(w, h - lowBandTop))
        drawRect(color = Color(0x6642A5F5), topLeft = androidx.compose.ui.geometry.Offset(0f, urgentBandTop), size = androidx.compose.ui.geometry.Size(w, h - urgentBandTop))

        if (history.isNotEmpty()) {
            val times = history.map { it.timestamp }
            val minT = times.min()
            val maxT = times.max()
            val spanT = (maxT - minT).coerceAtLeast(1L)
            val path = Path()
            history.forEachIndexed { idx, r ->
                val x = ((r.timestamp - minT).toFloat() / spanT.toFloat()).coerceIn(0f, 1f) * w
                val y = yFor(if (units == Units.MG_DL) r.mgdl else r.mgdl / 18.0)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color(0xFF00E5FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
}

private val mgdlFormat = DecimalFormat("###0.#")
private fun format(v: Double) = mgdlFormat.format(v)

@androidx.annotation.VisibleForTesting
internal fun computeChartSeries(
    rawHistory: List<com.g7nightalarm.app.data.GlucoseReading>,
    latest: com.g7nightalarm.app.data.GlucoseReading?
): List<com.g7nightalarm.app.data.GlucoseReading> {
    if (rawHistory.isEmpty() && latest == null) return emptyList()
    // Sort strictly by timestamp
    val sorted = rawHistory
        .filter { it.mgdl > 0.0 }
        .sortedBy { it.timestamp }
    // For duplicate timestamps, keep the last value to align with what header shows as "latest"
    val byTs = java.util.LinkedHashMap<Long, com.g7nightalarm.app.data.GlucoseReading>()
    sorted.forEach { byTs[it.timestamp] = it }
    var out = byTs.values.toList()
    // Ensure the last point matches the displayed latest reading when available
    if (latest != null && latest.mgdl > 0.0) {
        if (out.isEmpty() || latest.timestamp > out.last().timestamp) {
            out = out + latest
        } else if (latest.timestamp == out.last().timestamp) {
            // Replace last with latest value if timestamps are equal
            out = out.dropLast(1) + latest
        }
    }
    return out
}

