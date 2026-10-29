package com.g7nightalarm.app.ui

import com.g7nightalarm.app.ui.screens.computeChartSeries
import com.g7nightalarm.app.data.GlucoseReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChartDataTest {
    @Test
    fun series_sorted_deduped_and_last_matches_latest() {
        val t0 = 1_000_000L
        val history = listOf(
            GlucoseReading(timestamp = t0 + 1000, mgdl = 110.0, slopeMgdlPerMin = null, slopeName = null, source = "t"),
            GlucoseReading(timestamp = t0 + 3000, mgdl = 120.0, slopeMgdlPerMin = null, slopeName = null, source = "t"),
            // Duplicate timestamp with earlier (should be overwritten by later value)
            GlucoseReading(timestamp = t0 + 3000, mgdl = 125.0, slopeMgdlPerMin = null, slopeName = null, source = "t"),
            // Out-of-order entry
            GlucoseReading(timestamp = t0 + 2000, mgdl = 115.0, slopeMgdlPerMin = null, slopeName = null, source = "t"),
            // Garbage/zero should be filtered out
            GlucoseReading(timestamp = t0 + 4000, mgdl = 0.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        )
        val latest = GlucoseReading(timestamp = t0 + 5000, mgdl = 142.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        val series = computeChartSeries(history, latest)
        // Ascending timestamps
        for (i in 1 until series.size) {
            assertTrue(series[i - 1].timestamp <= series[i].timestamp)
        }
        // No duplicate timestamps
        assertEquals(series.map { it.timestamp }.distinct().size, series.size)
        // Last equals latest
        assertEquals(latest.timestamp, series.last().timestamp)
        assertEquals(latest.mgdl, series.last().mgdl, 0.0)
    }
}

