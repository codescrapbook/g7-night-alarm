package com.g7nightalarm.app.ui

import com.google.common.truth.Truth.assertThat
import com.g7nightalarm.app.ui.screens.computeChartSeries
import com.g7nightalarm.app.data.GlucoseReading
import org.junit.Test

class HomeChartSeriesLatestTest {
    private fun history(endNow: Long, endMgdl: Double): List<GlucoseReading> {
        val list = ArrayList<GlucoseReading>()
        for (i in 0 until 36) {
            val t = endNow - (36 - i) * 5L * 60L * 1000L
            list.add(GlucoseReading(timestamp = t, mgdl = 100.0, slopeMgdlPerMin = null, slopeName = "Flat", source = "test"))
        }
        list.add(GlucoseReading(timestamp = endNow - 5 * 60_000L, mgdl = endMgdl - 7.0, slopeMgdlPerMin = null, slopeName = "Flat", source = "test"))
        return list
    }

    @Test
    fun last_point_matches_latest_142() {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 142.0, slopeMgdlPerMin = 1.0, slopeName = "↗", source = "test")
        val out = computeChartSeries(history(now, 142.0), latest)
        assertThat(out.last().mgdl).isEqualTo(142.0)
        assertThat(out.last().timestamp).isEqualTo(now)
    }

    @Test
    fun last_point_matches_latest_76() {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 76.0, slopeMgdlPerMin = -1.4, slopeName = "↘", source = "test")
        val out = computeChartSeries(history(now, 76.0), latest)
        assertThat(out.last().mgdl).isEqualTo(76.0)
        assertThat(out.last().timestamp).isEqualTo(now)
    }

    @Test
    fun last_point_matches_latest_120() {
        val now = System.currentTimeMillis()
        val latest = GlucoseReading(timestamp = now, mgdl = 120.0, slopeMgdlPerMin = 0.0, slopeName = "→", source = "test")
        val out = computeChartSeries(history(now, 120.0), latest)
        assertThat(out.last().mgdl).isEqualTo(120.0)
        assertThat(out.last().timestamp).isEqualTo(now)
    }
}

