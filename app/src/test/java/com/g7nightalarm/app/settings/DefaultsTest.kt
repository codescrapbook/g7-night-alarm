package com.g7nightalarm.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultsTest {
    @Test
    fun settings_state_defaults_match_spec() {
        val s = SettingsState()
        assertEquals(70.0, s.thresholds.urgentLow, 0.0)
        assertEquals(80.0, s.thresholds.low, 0.0)
        assertEquals(250.0, s.thresholds.high, 0.0)
        assertEquals(3.0, s.thresholds.rapidFallRateMgdlPerMin, 0.0)
        assertEquals(30, s.thresholds.noDataMinutes)
        assertTrue(s.voiceEnabled)
    }
}

