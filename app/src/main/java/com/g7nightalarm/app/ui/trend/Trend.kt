package com.g7nightalarm.app.ui.trend

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.data.GlucoseReading
import kotlin.math.abs

enum class TrendDirection {
    DOUBLE_UP, SINGLE_UP, FORTY_FIVE_UP,
    FLAT,
    FORTY_FIVE_DOWN, SINGLE_DOWN, DOUBLE_DOWN,
    UNKNOWN
}

data class Trend(val direction: TrendDirection)

private fun normalizeName(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val trimmed = name.trim()
    // Accept glyphs as-is
    val glyph = when (trimmed) {
        "↑↑" -> "doubleup"
        "↑" -> "singleup"
        "↗" -> "fortyfiveup"
        "→" -> "flat"
        "↘" -> "fortyfivedown"
        "↓" -> "singledown"
        "↓↓" -> "doubledown"
        else -> null
    }
    if (glyph != null) return glyph
    // Canonicalize Dexcom/xDrip names
    return trimmed
        .lowercase()
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
}

@VisibleForTesting
fun mapSlopeNameToDirection(name: String?): TrendDirection {
    val n = normalizeName(name)
    return when (n) {
        "doubleup" -> TrendDirection.DOUBLE_UP
        "singleup" -> TrendDirection.SINGLE_UP
        "fortyfiveup" -> TrendDirection.FORTY_FIVE_UP
        "flat" -> TrendDirection.FLAT
        "fortyfivedown" -> TrendDirection.FORTY_FIVE_DOWN
        "singledown" -> TrendDirection.SINGLE_DOWN
        "doubledown" -> TrendDirection.DOUBLE_DOWN
        "notcomputable", "rateoutofrange" -> TrendDirection.UNKNOWN
        else -> TrendDirection.UNKNOWN
    }
}

@VisibleForTesting
fun mapSlopeValueToDirection(slopeMgdlPerMin: Double): TrendDirection {
    return when {
        slopeMgdlPerMin > 3.0 -> TrendDirection.DOUBLE_UP
        slopeMgdlPerMin >= 2.0 -> TrendDirection.SINGLE_UP
        slopeMgdlPerMin >= 1.0 -> TrendDirection.FORTY_FIVE_UP
        slopeMgdlPerMin > -1.0 && slopeMgdlPerMin < 1.0 -> TrendDirection.FLAT
        slopeMgdlPerMin <= -1.0 && slopeMgdlPerMin > -2.0 -> TrendDirection.FORTY_FIVE_DOWN
        slopeMgdlPerMin <= -2.0 && slopeMgdlPerMin >= -3.0 -> TrendDirection.SINGLE_DOWN
        slopeMgdlPerMin < -3.0 -> TrendDirection.DOUBLE_DOWN
        else -> TrendDirection.UNKNOWN
    }
}

fun deriveTrend(latest: GlucoseReading?, previous: GlucoseReading? = null): Trend {
    if (latest == null) return Trend(TrendDirection.UNKNOWN)
    // 1) xDrip/Dexcom slopeName priority (accept glyphs too)
    val fromName = mapSlopeNameToDirection(latest.slopeName)
    if (fromName != TrendDirection.UNKNOWN) return Trend(fromName)
    // 2) Provided numeric slope
    val slope = latest.slopeMgdlPerMin
    if (slope != null) return Trend(mapSlopeValueToDirection(slope))
    // 3) Fallback to computed slope from previous
    if (previous != null && previous.timestamp > 0L && previous.timestamp != latest.timestamp) {
        val dtMin = abs((latest.timestamp - previous.timestamp).toDouble()) / 60000.0
        if (dtMin > 0.0001) {
            val s = (latest.mgdl - previous.mgdl) / dtMin
            return Trend(mapSlopeValueToDirection(s))
        }
    }
    // 4) Unknown
    return Trend(TrendDirection.UNKNOWN)
}

fun Trend.label(): String = when (direction) {
    TrendDirection.DOUBLE_UP -> "Rising fast"
    TrendDirection.SINGLE_UP -> "Rising"
    TrendDirection.FORTY_FIVE_UP -> "Rising slowly"
    TrendDirection.FLAT -> "Steady"
    TrendDirection.FORTY_FIVE_DOWN -> "Falling slowly"
    TrendDirection.SINGLE_DOWN -> "Falling"
    TrendDirection.DOUBLE_DOWN -> "Falling fast"
    TrendDirection.UNKNOWN -> "Trend unknown"
}

fun Trend.contentDescription(): String = when (direction) {
    TrendDirection.DOUBLE_UP -> "Rising fast, more than 3 milligrams per deciliter per minute"
    TrendDirection.SINGLE_UP -> "Rising, 2 to 3 milligrams per deciliter per minute"
    TrendDirection.FORTY_FIVE_UP -> "Rising slowly, 1 to 2 milligrams per deciliter per minute"
    TrendDirection.FLAT -> "Steady, less than 1 milligram per deciliter per minute"
    TrendDirection.FORTY_FIVE_DOWN -> "Falling slowly, 1 to 2 milligrams per deciliter per minute"
    TrendDirection.SINGLE_DOWN -> "Falling, 2 to 3 milligrams per deciliter per minute"
    TrendDirection.DOUBLE_DOWN -> "Falling fast, more than 3 milligrams per deciliter per minute"
    TrendDirection.UNKNOWN -> "Trend unknown"
}

fun Trend.symbol(): String = when (direction) {
    TrendDirection.DOUBLE_UP -> "↑↑"
    TrendDirection.SINGLE_UP -> "↑"
    TrendDirection.FORTY_FIVE_UP -> "↗"
    TrendDirection.FLAT -> "→"
    TrendDirection.FORTY_FIVE_DOWN -> "↘"
    TrendDirection.SINGLE_DOWN -> "↓"
    TrendDirection.DOUBLE_DOWN -> "↓↓"
    TrendDirection.UNKNOWN -> "·"
}

@Composable
fun TrendArrowIcon(
    direction: TrendDirection,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp
) {
    // Draw simple thick arrows using Canvas; duplicate for "double" variants.
    Canvas(modifier = modifier) {
        val w = size.toPx()
        val h = size.toPx()
        val sw = strokeWidth.toPx()
        val shaftLen = h * 0.55f
        val headSize = h * 0.18f
        fun drawArrow(angleDeg: Float, offset: Offset = Offset.Zero) {
            rotate(degrees = angleDeg, pivot = center) {
                // Vertical up arrow centered at canvas center + offset
                val cx = center.x + offset.x
                val cy = center.y + offset.y
                // Shaft
                drawLine(
                    color = color,
                    start = Offset(cx, cy + shaftLen * 0.3f),
                    end = Offset(cx, cy - shaftLen * 0.7f),
                    strokeWidth = sw
                )
                // Head (triangle)
                val path = Path().apply {
                    moveTo(cx, cy - shaftLen * 0.7f - headSize)
                    lineTo(cx - headSize, cy - shaftLen * 0.7f + headSize)
                    lineTo(cx + headSize, cy - shaftLen * 0.7f + headSize)
                    close()
                }
                drawPath(path = path, color = color)
            }
        }
        when (direction) {
            TrendDirection.DOUBLE_UP -> {
                drawArrow(0f, Offset(-w * 0.08f, 0f))
                drawArrow(0f, Offset(w * 0.08f, 0f))
            }
            TrendDirection.SINGLE_UP -> drawArrow(0f)
            TrendDirection.FORTY_FIVE_UP -> drawArrow(-45f)
            TrendDirection.FLAT -> {
                // Horizontal → arrow
                rotate(90f) { drawArrow(0f) }
            }
            TrendDirection.FORTY_FIVE_DOWN -> drawArrow(225f) // -135
            TrendDirection.SINGLE_DOWN -> drawArrow(180f)
            TrendDirection.DOUBLE_DOWN -> {
                drawArrow(180f, Offset(-w * 0.08f, 0f))
                drawArrow(180f, Offset(w * 0.08f, 0f))
            }
            TrendDirection.UNKNOWN -> { /* nothing */ }
        }
    }
}

