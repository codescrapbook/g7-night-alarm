package com.g7nightalarm.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.ui.theme.G7Theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import com.g7nightalarm.app.ui.alarm.AlarmContent
import com.g7nightalarm.app.receiver.AlarmActionReceiver

object AlarmScreens {
    @Composable
    fun Urgent() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AlarmContent(type = AlarmActionReceiver.TYPE_LOW, onDismiss = {})
        }
    }
    @Composable
    fun LowWarn() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AlarmContent(type = AlarmActionReceiver.TYPE_LOW_WARN, onDismiss = {})
        }
    }

    @Composable
    fun High() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AlarmContent(type = AlarmActionReceiver.TYPE_HIGH, onDismiss = {})
        }
    }

    @Composable
    fun SignalLost() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AlarmContent(type = AlarmActionReceiver.TYPE_SIGNAL, onDismiss = {})
        }
    }
}

