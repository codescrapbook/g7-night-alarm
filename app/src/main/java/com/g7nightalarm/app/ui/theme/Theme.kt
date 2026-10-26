package com.g7nightalarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.g7nightalarm.app.R

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DBB63),       // swampy green
    onPrimary = Color(0xFF0D1B0D),
    secondary = Color(0xFF8E44AD),     // bruised purple
    onSecondary = Color(0xFFEDE7F6),
    background = Color(0xFF0A0F0A),    // very dark moss
    onBackground = Color(0xFFE5E5E5),  // bone white
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE5E5E5),
    error = Color(0xFFC62828)          // blood red accents
)

private val UndeadHeaders = FontFamily(Font(resId = R.font.creepster_regular))
private val DefaultTypography = Typography()
private val UndeadTypography = Typography(
    displayLarge = DefaultTypography.displayLarge, // keep numeric header plain
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = UndeadHeaders),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = UndeadHeaders),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = UndeadHeaders)
)

@Composable
fun G7Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = UndeadTypography,
        content = content
    )
}

