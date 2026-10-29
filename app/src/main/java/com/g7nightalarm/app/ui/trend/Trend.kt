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
import kotlin.math.hypot
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
/** Wide enough for two heavy shafts with a small gap. Height stays 0.70em. */
internal val TrendArrowWidthDouble = 1.30.em

/** Shaft thickness as a fraction of the arrow's own box (the smaller side). */
internal const val ArrowShaftFraction = 0.22f
/** Arrowhead width as a fraction of the arrow's own box width. */
internal const val ArrowHeadWidthFraction = 0.70f
/** Share of the arrow length taken by the head. The rest is the shaft. */
internal const val ArrowHeadLengthFraction = 0.78f
/** Gap between the two arrows, as a fraction of the shorter box side. */
internal const val ArrowDoubleGapFraction = 0.06f

private const val TrendInlineAlternate = "[trend]"

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
        arrowPolygons(direction, w, h).forEach { pts ->
            if (pts.size < 3) return@forEach
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                close()
            }
            drawPath(path, color)
        }
    }
}

/**
 * Filled arrow polygons in the placeholder box. One polygon for a single arrow,
 * two side by side for the doubles. Area is the shoelace of these polygons —
 * a hairline stroke cannot satisfy [arrowFillFraction].
 */
@VisibleForTesting
internal fun arrowPolygons(direction: TrendDirection, w: Float, h: Float): List<List<Offset>> {
    val deg = rotationDegreesFor(direction) ?: return emptyList()
    val doubles = direction == TrendDirection.DOUBLE_UP || direction == TrendDirection.DOUBLE_DOWN
    val cells = if (doubles) 2 else 1
    val gap = if (doubles) ArrowDoubleGapFraction * min(w, h) else 0f
    val cellW = (w - gap * (cells - 1)) / cells
    val pivotX = w / 2f
    val pivotY = h / 2f
    return (0 until cells).map { i ->
        val cx = i * (cellW + gap) + cellW / 2f
        val headHalf = ArrowHeadWidthFraction * cellW / 2f
        val shaftHalf = ArrowShaftFraction * min(cellW, h) / 2f
        longestArrow(cx, h / 2f, headHalf, shaftHalf, deg, pivotX, pivotY, w, h)
    }
}

/**
 * Grows the arrow along its axis until the filled shape meets the box.
 * Shaft width and head width stay fixed, so a 45° arrow stays heavy and
 * reaches the edges instead of sitting in the middle of the placeholder.
 */
private fun longestArrow(
    cx: Float,
    cy: Float,
    headHalf: Float,
    shaftHalf: Float,
    deg: Float,
    pivotX: Float,
    pivotY: Float,
    w: Float,
    h: Float
): List<Offset> {
    fun placed(halfLen: Float): List<Offset> {
        val up = upArrowPolygon(cx, cy - halfLen, cy + halfLen, headHalf, shaftHalf)
        return up.map { rotateClockwise(it, deg, pivotX, pivotY) }
    }
    var best = placed(h / 2f)
    var lo = h / 2f
    var hi = hypot(w, h)
    repeat(24) {
        val mid = (lo + hi) / 2f
        val pts = placed(mid)
        if (contained(pts, w, h)) {
            best = pts
            lo = mid
        } else {
            hi = mid
        }
    }
    return best
}

private fun contained(points: List<Offset>, w: Float, h: Float): Boolean {
    for (p in points) {
        if (p.x < -0.5f || p.y < -0.5f || p.x > w + 0.5f || p.y > h + 0.5f) return false
    }
    return true
}

/** Filled area divided by the placeholder box. Independent of bitmaps. */
@VisibleForTesting
internal fun arrowFillFraction(direction: TrendDirection, widthPx: Float, heightPx: Float): Float {
    if (widthPx <= 0f || heightPx <= 0f) return 0f
    var area = 0f
    for (poly in arrowPolygons(direction, widthPx, heightPx)) {
        area += polygonArea(poly)
    }
    return area / (widthPx * heightPx)
}

private fun upArrowPolygon(
    cx: Float,
    top: Float,
    bottom: Float,
    headHalfW: Float,
    shaftHalfW: Float
): List<Offset> {
    val baseY = top + (bottom - top) * ArrowHeadLengthFraction
    val shaft = shaftHalfW.coerceAtMost(headHalfW)
    return listOf(
        Offset(cx, top),
        Offset(cx - headHalfW, baseY),
        Offset(cx - shaft, baseY),
        Offset(cx - shaft, bottom),
        Offset(cx + shaft, bottom),
        Offset(cx + shaft, baseY),
        Offset(cx + headHalfW, baseY)
    )
}

private fun rotateClockwise(p: Offset, degrees: Float, cx: Float, cy: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    val dx = p.x - cx
    val dy = p.y - cy
    return Offset(cx + dx * c - dy * s, cy + dx * s + dy * c)
}

private fun polygonArea(points: List<Offset>): Float {
    var sum = 0f
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        sum += a.x * b.y - b.x * a.y
    }
    return abs(sum) * 0.5f
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
            appendInlineContent("trend", TrendInlineAlternate)
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
                AnnotatedString.Range(
                    placeholder,
                    placeholderStart,
                    placeholderStart + TrendInlineAlternate.length
                )
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

