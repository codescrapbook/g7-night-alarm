package com.g7nightalarm.app.ui.trend

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.ui.a11y.fontSizeSp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

@VisibleForTesting
fun rotationDegreesFor(direction: TrendDirection): Float? = when (direction) {
    TrendDirection.DOUBLE_UP, TrendDirection.SINGLE_UP -> 0f
    TrendDirection.FORTY_FIVE_UP -> 45f // NE
    TrendDirection.FLAT -> 90f // →
    TrendDirection.FORTY_FIVE_DOWN -> 135f // SE
    TrendDirection.SINGLE_DOWN, TrendDirection.DOUBLE_DOWN -> 180f // ↓
    TrendDirection.UNKNOWN -> null
}

/** Cap-height of the reading digits. 0.78em is taller than the glyph and hangs below the baseline. */
internal val TrendArrowHeight = 0.70.em
internal val TrendArrowWidthSingle = 0.78.em
internal val TrendArrowWidthDouble = 1.15.em

@Composable
fun TrendArrowIcon(
    direction: TrendDirection,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    if (direction == TrendDirection.UNKNOWN) return
    val canvasModifier = if (size != null) modifier.size(size) else modifier
    Canvas(canvasModifier) {
        val w = this.size.width
        val h = this.size.height
        if (w <= 1f || h <= 1f) return@Canvas
        val deg = rotationDegreesFor(direction) ?: return@Canvas
        val sw = 0.14f * min(w, h)
        val headHalfW = 0.22f * min(w, h)
        val gap = 0.16f * min(w, h)
        val doubles = direction == TrendDirection.DOUBLE_UP || direction == TrendDirection.DOUBLE_DOWN
        val offsets = if (doubles) {
            val sep = min((w / 2f) - headHalfW, headHalfW + gap / 2f).coerceAtLeast(sw)
            listOf(-sep, sep)
        } else {
            listOf(0f)
        }
        val half = edgeHalfLength(deg, w, h) * 0.96f
        offsets.forEach { ox ->
            drawUpArrow(deg, ox, half, headHalfW, sw, color)
        }
    }
}

/** Half-length of an arrow centered in the box so the tip and tail land on the edges after rotation. */
private fun edgeHalfLength(angleDeg: Float, w: Float, h: Float): Float {
    val rad = Math.toRadians(angleDeg.toDouble())
    val ux = sin(rad).toFloat()
    val uy = (-cos(rad)).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val tip = travel(cx, cy, ux, uy, w, h)
    val tail = travel(cx, cy, -ux, -uy, w, h)
    return min(tip, tail)
}

private fun travel(x: Float, y: Float, dx: Float, dy: Float, w: Float, h: Float): Float {
    val tx = when {
        dx > 1e-4f -> (w - x) / dx
        dx < -1e-4f -> (0f - x) / dx
        else -> Float.POSITIVE_INFINITY
    }
    val ty = when {
        dy > 1e-4f -> (h - y) / dy
        dy < -1e-4f -> (0f - y) / dy
        else -> Float.POSITIVE_INFINITY
    }
    return min(tx, ty)
}

private fun DrawScope.drawUpArrow(
    angleDeg: Float,
    offsetX: Float,
    half: Float,
    headHalfW: Float,
    strokeWidth: Float,
    color: Color
) {
    if (half <= strokeWidth) return
    rotate(degrees = angleDeg, pivot = center) {
        val cx = center.x + offsetX
        val cy = center.y
        val tipY = cy - half
        val tailY = cy + half
        val headLen = (tailY - tipY) * 0.42f
        val baseY = tipY + headLen
        drawLine(
            color = color,
            start = Offset(cx, tailY),
            end = Offset(cx, baseY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
        val head = Path().apply {
            moveTo(cx, tipY)
            lineTo(cx - headHalfW, baseY)
            lineTo(cx + headHalfW, baseY)
            close()
        }
        drawPath(head, color)
    }
}

/**
 * Reading value and trend arrow as one text line. The placeholder is cap-height
 * and sits on the baseline, so the arrow matches the digits instead of hanging below them.
 */
@Composable
fun InlineTrendReading(
    reading: String,
    direction: TrendDirection,
    color: Color,
    style: TextStyle,
    contentDescription: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val showArrow = direction != TrendDirection.UNKNOWN
    val arrowWidth = if (direction == TrendDirection.DOUBLE_UP || direction == TrendDirection.DOUBLE_DOWN) {
        TrendArrowWidthDouble
    } else {
        TrendArrowWidthSingle
    }
    val placeholder = Placeholder(
        width = arrowWidth,
        height = TrendArrowHeight,
        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
    )
    val annotated = buildAnnotatedString {
        append(reading)
        if (showArrow) {
            append(" ")
            appendInlineContent("trend", "[trend]")
        }
    }
    val inline = if (showArrow) {
        mapOf(
            "trend" to InlineTextContent(placeholder) {
                TrendArrowIcon(
                    direction = direction,
                    color = color,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        emptyMap()
    }
    val measurer = rememberTextMeasurer()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.5f)
    // Visual floor of 40sp. Dividing by fontScale keeps that floor on screen at fontScale 2
    // without dropping the arrow onto a second line on a phone.
    val minSize = (40f / fontScale).sp
    val baseStyle = style.copy(
        color = color,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold
    )
    BoxWithConstraints(modifier) {
        val fitted = if (!showArrow) {
            baseStyle.fontSize
        } else {
            fitReadingSize(
                measurer = measurer,
                text = annotated,
                style = baseStyle,
                placeholder = placeholder,
                placeholderStart = reading.length + 1,
                maxWidthPx = constraints.maxWidth,
                start = baseStyle.fontSize,
                min = minSize
            )
        }
        Text(
            text = annotated,
            inlineContent = inline,
            style = baseStyle.copy(fontSize = fitted),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            onTextLayout = onTextLayout,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .semantics {
                    this.contentDescription = contentDescription
                    fontSizeSp = fitted.value
                }
        )
    }
}

private fun fitReadingSize(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: AnnotatedString,
    style: TextStyle,
    placeholder: Placeholder,
    placeholderStart: Int,
    maxWidthPx: Int,
    start: TextUnit,
    min: TextUnit
): TextUnit {
    if (maxWidthPx <= 0 || !start.isSp || !min.isSp) return start
    fun overflows(size: TextUnit): Boolean {
        val layout = measurer.measure(
            text = text,
            style = style.copy(fontSize = size),
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1,
            placeholders = listOf(
                AnnotatedString.Range(placeholder, placeholderStart, placeholderStart + 1)
            ),
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        return layout.hasVisualOverflow
    }
    if (!overflows(start)) return start
    if (overflows(min)) return min
    var lo = min.value
    var hi = start.value
    repeat(12) {
        val mid = (lo + hi) / 2f
        if (overflows(mid.sp)) hi = mid else lo = mid
    }
    return lo.sp
}

