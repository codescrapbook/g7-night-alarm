package com.g7nightalarm.app.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.alarm.AlarmContent
import com.g7nightalarm.app.ui.theme.G7Theme
import com.g7nightalarm.app.ui.trend.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode.NATIVE
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * The arrow is an inline placeholder on the reading line. These checks use TextLayoutResult
 * only — no bitmaps — so they stay deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(NATIVE)
class AlarmTrendInlineAlignmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val directions = listOf(
        TrendDirection.DOUBLE_UP to "DoubleUp",
        TrendDirection.SINGLE_UP to "SingleUp",
        TrendDirection.FORTY_FIVE_UP to "FortyFiveUp",
        TrendDirection.FLAT to "Flat",
        TrendDirection.FORTY_FIVE_DOWN to "FortyFiveDown",
        TrendDirection.SINGLE_DOWN to "SingleDown",
        TrendDirection.DOUBLE_DOWN to "DoubleDown",
    )

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_fontScale1_arrow_is_centered_on_the_digits() = assertAllDirections(411.dp, 891.dp, 1f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_fontScale2_arrow_is_centered_on_the_digits() = assertAllDirections(411.dp, 891.dp, 2f)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet_fontScale1_arrow_is_centered_on_the_digits() = assertAllDirections(1280.dp, 800.dp, 1f)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet_fontScale2_arrow_is_centered_on_the_digits() = assertAllDirections(1280.dp, 800.dp, 2f)

    private fun assertAllDirections(
        width: androidx.compose.ui.unit.Dp,
        height: androidx.compose.ui.unit.Dp,
        fontScale: Float
    ) {
        directions.forEach { (direction, slopeName) ->
            val layoutRef = AtomicReference<TextLayoutResult>()
            val latest = GlucoseReading(
                timestamp = System.currentTimeMillis() - 60_000L,
                mgdl = 58.0,
                slopeMgdlPerMin = null,
                slopeName = slopeName,
                source = "test"
            )
            composeRule.setContent {
                val base = LocalDensity.current
                G7Theme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base.density, fontScale = fontScale)
                    ) {
                        Box(Modifier.size(width, height)) {
                            AlarmContent(
                                type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_URGENT,
                                onDismiss = {},
                                animationsEnabled = false,
                                previewLatest = latest,
                                previewUnits = Units.MG_DL,
                                onReadingLayout = { layoutRef.set(it) }
                            )
                        }
                    }
                }
            }
            composeRule.waitForIdle()
            val layout = layoutRef.get()
            assertNotNull("No reading layout for $direction @ fontScale $fontScale", layout)
            assertArrowAligned(layout!!, "$direction fontScale=$fontScale")
        }
    }

    private fun assertArrowAligned(layout: TextLayoutResult, label: String) {
        val density = layout.layoutInput.density
        val fontPx = with(density) { layout.layoutInput.style.fontSize.toPx() }
        val digit = layout.getBoundingBox(0)
        // getBoundingBox is the line box, which is taller than the glyph. Use it only when it
        // looks like a glyph; otherwise fall back to 0.7×fontSize as the cap height.
        val capHeight = if (digit.height in (fontPx * 0.45f)..(fontPx * 0.9f)) {
            digit.height
        } else {
            fontPx * 0.7f
        }
        val baseline = layout.getLineBaseline(0)
        val expectedCenter = baseline - capHeight / 2f
        val ph = layout.placeholderRects.firstOrNull()
        assertNotNull("$label missing placeholder rects=${layout.placeholderRects}", ph)
        val rect = ph!!
        val tol = with(density) { 2.dp.toPx() }
        val center = (rect.top + rect.bottom) / 2f
        assertTrue(
            "$label center delta=${center - expectedCenter} tol=$tol cap=$capHeight phH=${rect.height} font=$fontPx digitH=${digit.height}",
            abs(center - expectedCenter) <= tol
        )
        assertTrue(
            "$label height ratio=${rect.height / capHeight} phH=${rect.height} cap=$capHeight",
            rect.height in (capHeight * 0.95f)..(capHeight * 1.15f)
        )
        assertTrue(
            "$label top ph=${rect.top} digitTop=${digit.top} tol=$tol",
            rect.top >= digit.top - tol
        )
        assertTrue(
            "$label bottom ph=${rect.bottom} baseline=$baseline tol=$tol",
            rect.bottom <= baseline + tol
        )
        assertEquals("$label should be one line", 1, layout.lineCount)
        val placeholderIndex = "58 mg/dL".length + 1
        assertEquals("$label placeholder line", 0, layout.getLineForOffset(placeholderIndex))
    }
}
