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

object AlarmScreens {
    @Composable
    fun Urgent() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFB00020).copy(alpha = 0.6f))
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("URGENT ALARM", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                Text("Press and hold to dismiss", color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .background(color = Color(0xFFE5E5E5), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "HOLD",
                        // Plain, high-contrast text (no decorative font)
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
    @Composable
    fun LowWarn() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1976D2).copy(alpha = 0.15f))
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("LOW WARNING", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                Text("Glucose 75 mg/dL in warning band", color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }

    @Composable
    fun High() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFBC02D).copy(alpha = 0.15f))
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HIGH ALARM", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                Text("Glucose high", color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }

    @Composable
    fun SignalLost() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF455A64).copy(alpha = 0.15f))
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SIGNAL LOST", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                Text("No data for 30+ minutes", color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

