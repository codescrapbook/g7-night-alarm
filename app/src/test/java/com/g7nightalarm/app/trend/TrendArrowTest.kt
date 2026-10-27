package com.g7nightalarm.app.trend

import com.g7nightalarm.app.ui.trend.TrendDirection
import com.g7nightalarm.app.ui.trend.mapSlopeNameToDirection
import com.g7nightalarm.app.ui.trend.mapSlopeValueToDirection
import org.junit.Assert.assertEquals
import org.junit.Test

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
}

