package com.g7nightalarm.app.ui.alarm

import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {
    private lateinit var alarmController: AlarmController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmController = AlarmController(this)
        setContent {
            G7Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AlarmContent(onDismiss = {
                        alarmController.stopAlarm()
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
private fun AlarmContent(onDismiss: () -> Unit) {
    val flashing = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            flashing.animateTo(1f, animationSpec = tween(500))
            flashing.animateTo(0f, animationSpec = tween(500))
        }
    }
    val bg = Color(0xFFB00020).copy(alpha = 0.4f + 0.6f * flashing.value)
    val holding = remember { mutableStateOf(false) }
    val holdMs = 1500L
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("URGENT ALARM", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
        Text("Press and hold to dismiss", color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
        Box(
            modifier = Modifier
                .padding(top = 48.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF5252))
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
            Text("HOLD", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

