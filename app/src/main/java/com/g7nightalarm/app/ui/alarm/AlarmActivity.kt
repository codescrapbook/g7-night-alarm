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
    val bgBase = when (type) {
        AlarmActionReceiver.TYPE_HIGH -> Color(0xFFFBC02D)
        AlarmActionReceiver.TYPE_SIGNAL -> Color(0xFF455A64)
        AlarmActionReceiver.TYPE_LOW_WARN -> Color(0xFF1976D2)
        else -> Color(0xFFB00020) // urgent/low default
    }
    val bg = if (animationsEnabled) bgBase.copy(alpha = 0.4f + 0.6f * flashing.value) else bgBase.copy(alpha = 0.8f)
    val holding = remember { mutableStateOf(false) }
    val holdMs = 1500L
    val (title, subtitle, dismissLabel) = when (type) {
        AlarmActionReceiver.TYPE_LOW_WARN -> Triple("LOW WARNING", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_HIGH -> Triple("HIGH ALARM", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_SIGNAL -> Triple("SIGNAL LOST", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_LOW -> Triple("LOW ALARM", "Press and hold to dismiss", "HOLD")
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
        Text(title, style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Default), fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
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

