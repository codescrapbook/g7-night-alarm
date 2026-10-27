package com.g7nightalarm.app.ui.alarm

import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.delay
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.util.UnitsUtil

class AlarmActivity : ComponentActivity() {
    private lateinit var alarmController: AlarmController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }
        // Keep screen on while alarm is showing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        alarmController = AlarmController(this)
        val type = intent?.getStringExtra(AlarmActionReceiver.EXTRA_TYPE)
        setContent {
            G7Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AlarmContent(
                        type = type,
                        onDismiss = {
                        alarmController.stopAlarm()
                        // notify service to resume voice
                        sendBroadcast(android.content.Intent(com.g7nightalarm.app.service.MonitorService.ACTION_ALARM_STOPPED))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun AlarmContent(
    type: String?,
    onDismiss: () -> Unit,
    animationsEnabled: Boolean = true,
    previewLatest: GlucoseReading? = null,
    previewUnits: Units? = null
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).glucoseDao() }
    val settingsRepo = remember { SettingsRepository(context) }
    val flowLatest by remember { dao.latestFlow() }.collectAsState(initial = null)
    val s by settingsRepo.flow.collectAsState(initial = com.g7nightalarm.app.settings.SettingsState())
    val latest = previewLatest ?: flowLatest
    val units = previewUnits ?: s.units
    val ageMin = latest?.let { ((System.currentTimeMillis() - it.timestamp) / 60000.0).toInt().coerceAtLeast(0) } ?: 0
    fun trendArrow(): String {
        val name = latest?.slopeName ?: ""
        if (name.contains("↗") || name.contains("↘") || name.contains("→")) return name
        val slope = latest?.slopeMgdlPerMin ?: 0.0
        return when {
            slope > 0.5 -> "↗"
            slope < -0.5 -> "↘"
            else -> "→"
        }
    }
    val readingTitle = if (latest == null) {
        "— ${if (units == Units.MG_DL) "mg/dL" else "mmol/L"}"
    } else {
        "${UnitsUtil.formatValue(latest!!.mgdl, units)} ${if (units == Units.MG_DL) "mg/dL" else "mmol/L"} ${trendArrow()}"
    }
    val readingSubtitle = if (latest == null) "" else "${ageMin}m ago"

    val flashing = remember { Animatable(0f) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            while (true) {
                flashing.animateTo(1f, animationSpec = tween(500))
                flashing.animateTo(0f, animationSpec = tween(500))
            }
        }
    }
    val (cA, cB, titleColor, subtitleColor) = when (type) {
        AlarmActionReceiver.TYPE_URGENT -> Quad(Color(0xFF8E0000), Color(0xFFB71C1C), Color.White, Color(0xFFE0E0E0))
        AlarmActionReceiver.TYPE_LOW -> Quad(Color(0xFFAA2E00), Color(0xFFD84315), Color.White, Color(0xFFE0E0E0))
        AlarmActionReceiver.TYPE_HIGH -> Quad(Color(0xFFFFA000), Color(0xFFFF8F00), Color.Black, Color(0xFF1A1A1A))
        AlarmActionReceiver.TYPE_SIGNAL -> Quad(Color(0xFF37474F), Color(0xFF455A64), Color.White, Color(0xFFE0E0E0))
        AlarmActionReceiver.TYPE_LOW_WARN -> Quad(Color(0xFF0D47A1), Color(0xFF1976D2), Color.White, Color(0xFFE0E0E0))
        else -> Quad(Color(0xFF8E0000), Color(0xFFB71C1C), Color.White, Color(0xFFE0E0E0))
    }
    val bg = if (animationsEnabled) androidx.compose.ui.graphics.lerp(cA, cB, flashing.value) else cB
    val holding = remember { mutableStateOf(false) }
    val holdMs = 1500L
    val (title, subtitle, dismissLabel) = when (type) {
        AlarmActionReceiver.TYPE_LOW_WARN -> Triple("LOW WARNING", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_HIGH -> Triple("HIGH ALARM", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_SIGNAL -> Triple("SIGNAL LOST", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_LOW -> Triple("LOW ALARM", "Press and hold to dismiss", "HOLD")
        AlarmActionReceiver.TYPE_URGENT -> Triple("URGENT ALARM", "Press and hold to dismiss", "HOLD")
        else -> Triple("URGENT ALARM", "Press and hold to dismiss", "HOLD")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Default, color = titleColor),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            when (dismissLabel) {
                "DISMISS" -> "Tap to dismiss"
                else -> "Press and hold to dismiss"
            },
            color = subtitleColor,
            modifier = Modifier.padding(top = 12.dp)
        )
        // Current reading (large, high-contrast, plain font)
        Text(
            readingTitle,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Default, color = titleColor),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 24.dp)
        )
        if (readingSubtitle.isNotBlank()) {
            Text(readingSubtitle, color = subtitleColor, modifier = Modifier.padding(top = 8.dp))
        }
        Box(
            modifier = Modifier
                .padding(top = 48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE5E5E5))
                .padding(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            holding.value = true
                            val start = System.currentTimeMillis()
                            tryAwaitRelease()
                            val dur = System.currentTimeMillis() - start
                            holding.value = false
                            if (dur >= holdMs) {
                                onDismiss()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                dismissLabel,
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default),
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }
    }
}

// Helper record for color tuple
private data class Quad(val a: Color, val b: Color, val title: Color, val subtitle: Color)
private operator fun Quad.component1() = a
private operator fun Quad.component2() = b
private operator fun Quad.component3() = title
private operator fun Quad.component4() = subtitle

