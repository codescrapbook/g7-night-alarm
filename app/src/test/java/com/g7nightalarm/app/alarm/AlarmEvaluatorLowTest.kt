package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsState
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEvaluatorLowTest {
    @Test
    fun mgdl_65_triggers_low_event() {
        val eval = AlarmEvaluator()
        val s = SettingsState()
        val event = eval.evaluate(
            latest = GlucoseReading(
                timestamp = System.currentTimeMillis(),
                mgdl = 65.0,
                slopeMgdlPerMin = null,
                slopeName = null,
                source = "unit"
            ),
            previous = null,
            nowMs = System.currentTimeMillis(),
            settings = s
        )
        assertTrue(event is AlarmEvent.Low)
    }
}

