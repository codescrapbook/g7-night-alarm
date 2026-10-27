package com.g7nightalarm.app.ui.alarm

import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.delay
import com.g7nightalarm.app.receiver.AlarmActionReceiver

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
fun AlarmContent(type: String?, onDismiss: () -> Unit, animationsEnabled: Boolean = true) {
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
        Text(title, style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Default, color = titleColor), fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = subtitleColor, modifier = Modifier.padding(top = 16.dp))
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

