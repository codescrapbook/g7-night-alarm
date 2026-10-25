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
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.settings.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember { AppDatabase.get(context).glucoseDao() }
    val settingsRepo = remember { SettingsRepository(context) }
    val modeRepo = remember { ModeRepository(context) }
    val latest by remember { dao.latestFlow() }.collectAsState(initial = null)
    var hours by remember { mutableIntStateOf(24) }
    val settings by settingsRepo.flow.collectAsState(initial = com.g7nightalarm.app.settings.SettingsState())
    val isSleep = settings.sleepMode
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO) }
    val sinceMs = System.currentTimeMillis() - hours * 60L * 60L * 1000L
    val history by remember(hours) {
        dao.streamSince(sinceMs).map { it }
    }.collectAsState(initial = emptyList())

    val title = when {
        latest == null -> "— mg/dL"
        else -> "${format(latest!!.mgdl)} mg/dL ${latest!!.slopeName ?: ""}"
    }
    val ageMin = latest?.let { ((System.currentTimeMillis() - it.timestamp) / 60000.0).toInt() } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
                Text("Updated ${ageMin}m ago · Mode: ${if (isSleep) "Sleep" else "Awake"}", color = Color.Gray)
            }
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                scope.launch {
                    modeRepo.setMode(AppMode.SLEEP, ModeTrigger.BUTTON)
                    settingsRepo.update { it[SettingsKeys.sleepMode] = true }
                }
            }) { Text("Good Night") }
            Button(onClick = {
                scope.launch {
                    modeRepo.setMode(AppMode.AWAKE, ModeTrigger.BUTTON)
                    settingsRepo.update { it[SettingsKeys.sleepMode] = false }
                }
            }) { Text("Good Morning") }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Color(0xFF181818), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            GlucoseChart(history = history, low = 70.0, urgentLow = 55.0, high = 250.0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(3, 6, 12, 24).forEach { h ->
                Button(onClick = { hours = h }) { Text("${h}h") }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun GlucoseChart(
    history: List<com.g7nightalarm.app.data.GlucoseReading>,
    low: Double,
    urgentLow: Double,
    high: Double
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Draw threshold bands
        fun yFor(v: Double): Float {
            val minV = 40.0
            val maxV = 300.0
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
            val path = Path()
            history.sortedBy { it.timestamp }.forEachIndexed { idx, r ->
                val x = ((r.timestamp - minT).toFloat() / (maxT - minT).toFloat()).coerceIn(0f, 1f) * w
                val y = yFor(r.mgdl)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color(0xFF00E5FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
}

private val mgdlFormat = DecimalFormat("###0.#")
private fun format(v: Double) = mgdlFormat.format(v)

