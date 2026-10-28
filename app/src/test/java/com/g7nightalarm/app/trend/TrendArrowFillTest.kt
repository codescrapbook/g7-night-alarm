package com.g7nightalarm.app.trend

import com.g7nightalarm.app.ui.trend.TrendDirection
import com.g7nightalarm.app.ui.trend.arrowFillFraction
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arrow is a filled polygon. A hairline stroke covers only a few percent of the box;
 * the reading arrow has to cover at least 30%. Area is the shoelace of the same polygons
 * [com.g7nightalarm.app.ui.trend.TrendArrowIcon] draws — no bitmaps.
 */
class TrendArrowFillTest {
    @Test
    fun filled_arrow_covers_at_least_30_percent_of_its_box() {
        val single = 78f to 70f // 0.78em by 0.70em
        val double = 130f to 70f // 1.30em by 0.70em
        TrendDirection.entries.filter { it != TrendDirection.UNKNOWN }.forEach { direction ->
            val (w, h) = if (
                direction == TrendDirection.DOUBLE_UP || direction == TrendDirection.DOUBLE_DOWN
            ) double else single
            val fraction = arrowFillFraction(direction, w, h)
            assertTrue(
                "$direction fill=$fraction box=${w}x$h",
                fraction >= 0.30f
            )
            // Same shape at a larger font scale.
            val big = arrowFillFraction(direction, w * 4f, h * 4f)
            assertTrue("$direction scaled fill=$big", big >= 0.30f)
        }
    }
}
