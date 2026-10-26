package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsState
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEvaluatorTest {
    private val evaluator = AlarmEvaluator()
    private val settings = SettingsState()

    @Test
    fun urgentLowTriggers() {
        val now = 1_000_000L
        val r = GlucoseReading(timestamp = now, mgdl = 54.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        val ev = evaluator.evaluate(r, null, now, settings)
        assertTrue(ev is AlarmEvent.UrgentLow)
    }

    @Test
    fun noDataTriggers() {
        val now = 1_000_000L
        val r = GlucoseReading(timestamp = now - 31 * 60_000L, mgdl = 100.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        val ev = evaluator.evaluate(r, null, now, settings)
        assertTrue(ev is AlarmEvent.NoData)
    }

    @Test
    fun rapidFallTriggers() {
        val now = 1_000_000L
        val prev = GlucoseReading(timestamp = now - 5 * 60_000L, mgdl = 150.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        val cur = GlucoseReading(timestamp = now, mgdl = 100.0, slopeMgdlPerMin = null, slopeName = null, source = "t")
        val ev = evaluator.evaluate(cur, prev, now, settings)
        assertTrue(ev is AlarmEvent.RapidFall)
    }
}

