package com.g7nightalarm.app.trend

import com.g7nightalarm.app.ui.trend.TrendDirection
import com.g7nightalarm.app.ui.trend.mapSlopeNameToDirection
import com.g7nightalarm.app.ui.trend.mapSlopeValueToDirection
import com.g7nightalarm.app.ui.trend.rotationDegreesFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class TrendArrowTest {
    @Test
    fun slopeName_mapping_recognizes_dexcom_and_glyphs() {
        assertEquals(TrendDirection.DOUBLE_UP, mapSlopeNameToDirection("DoubleUp"))
        assertEquals(TrendDirection.SINGLE_UP, mapSlopeNameToDirection("SingleUp"))
        assertEquals(TrendDirection.FORTY_FIVE_UP, mapSlopeNameToDirection("FortyFiveUp"))
        assertEquals(TrendDirection.FLAT, mapSlopeNameToDirection("Flat"))
        assertEquals(TrendDirection.FORTY_FIVE_DOWN, mapSlopeNameToDirection("FortyFiveDown"))
        assertEquals(TrendDirection.SINGLE_DOWN, mapSlopeNameToDirection("SingleDown"))
        assertEquals(TrendDirection.DOUBLE_DOWN, mapSlopeNameToDirection("DoubleDown"))
        // Glyphs
        assertEquals(TrendDirection.DOUBLE_UP, mapSlopeNameToDirection("↑↑"))
        assertEquals(TrendDirection.SINGLE_UP, mapSlopeNameToDirection("↑"))
        assertEquals(TrendDirection.FORTY_FIVE_UP, mapSlopeNameToDirection("↗"))
        assertEquals(TrendDirection.FLAT, mapSlopeNameToDirection("→"))
        assertEquals(TrendDirection.FORTY_FIVE_DOWN, mapSlopeNameToDirection("↘"))
        assertEquals(TrendDirection.SINGLE_DOWN, mapSlopeNameToDirection("↓"))
        assertEquals(TrendDirection.DOUBLE_DOWN, mapSlopeNameToDirection("↓↓"))
        // Unknown/disabled
        assertEquals(TrendDirection.UNKNOWN, mapSlopeNameToDirection("NOT COMPUTABLE"))
        assertEquals(TrendDirection.UNKNOWN, mapSlopeNameToDirection("RATE OUT OF RANGE"))
    }

    @Test
    fun numeric_threshold_boundaries() {
        // Positive
        assertEquals(TrendDirection.FLAT, mapSlopeValueToDirection(0.99))
        assertEquals(TrendDirection.FORTY_FIVE_UP, mapSlopeValueToDirection(1.0))
        assertEquals(TrendDirection.SINGLE_UP, mapSlopeValueToDirection(2.0))
        assertEquals(TrendDirection.SINGLE_UP, mapSlopeValueToDirection(3.0))
        assertEquals(TrendDirection.DOUBLE_UP, mapSlopeValueToDirection(3.01))
        // Negative
        assertEquals(TrendDirection.FORTY_FIVE_DOWN, mapSlopeValueToDirection(-1.0))
        assertEquals(TrendDirection.SINGLE_DOWN, mapSlopeValueToDirection(-2.0))
        assertEquals(TrendDirection.SINGLE_DOWN, mapSlopeValueToDirection(-3.0))
        assertEquals(TrendDirection.DOUBLE_DOWN, mapSlopeValueToDirection(-3.01))
    }

    @Test
    fun orientation_degrees_and_tip_vectors() {
        // Check deterministic rotation degrees we draw with
        assertEquals(0f, rotationDegreesFor(TrendDirection.SINGLE_UP))
        assertEquals(0f, rotationDegreesFor(TrendDirection.DOUBLE_UP))
        assertEquals(45f, rotationDegreesFor(TrendDirection.FORTY_FIVE_UP))
        assertEquals(90f, rotationDegreesFor(TrendDirection.FLAT))
        assertEquals(135f, rotationDegreesFor(TrendDirection.FORTY_FIVE_DOWN))
        assertEquals(180f, rotationDegreesFor(TrendDirection.SINGLE_DOWN))
        assertEquals(180f, rotationDegreesFor(TrendDirection.DOUBLE_DOWN))
        // Tip vector checks: rotate the canonical up vector (0,-1) by degrees
        fun tipDeg(deg: Float): Pair<Double, Double> {
            val rad = deg * PI / 180.0
            val x = 0.0 * cos(rad) - (-1.0) * sin(rad) // rotate (0,-1)
            val y = 0.0 * sin(rad) + (-1.0) * cos(rad)
            return Pair(x, y)
        }
        fun assertSigns(pair: Pair<Double, Double>, expDxPositive: Boolean?, expDyNegative: Boolean?) {
            expDxPositive?.let { if (it) assertTrue(pair.first > 0) else assertTrue(pair.first < 0) }
            expDyNegative?.let { if (it) assertTrue(pair.second < 0) else assertTrue(pair.second > 0) }
        }
        // ↗ dx>0, dy<0
        assertSigns(tipDeg(rotationDegreesFor(TrendDirection.FORTY_FIVE_UP)!!), expDxPositive = true, expDyNegative = true)
        // ↘ dx>0, dy>0
        val d = tipDeg(rotationDegreesFor(TrendDirection.FORTY_FIVE_DOWN)!!)
        assertTrue(d.first > 0); assertTrue(d.second > 0)
        // → dx>0, dy≈0 (allow small magnitude tolerance by comparing abs(dy) < 1e-6)
        val flat = tipDeg(rotationDegreesFor(TrendDirection.FLAT)!!)
        assertTrue(flat.first > 0); assertTrue(kotlin.math.abs(flat.second) < 1e-6)
        // ↑ dy<0, ↓ dy>0
        val up = tipDeg(rotationDegreesFor(TrendDirection.SINGLE_UP)!!)
        assertTrue(up.second < 0)
        val down = tipDeg(rotationDegreesFor(TrendDirection.SINGLE_DOWN)!!)
        assertTrue(down.second > 0)
    }
}

