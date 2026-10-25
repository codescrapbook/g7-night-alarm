package com.g7nightalarm.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRampTest {
    @Test
    fun rampStartsAtStartPercent() {
        val v = AlarmRamp.computeRampedPercent(60, 0)
        assertEquals(60, v)
    }

    @Test
    fun rampEndsAt100() {
        val v = AlarmRamp.computeRampedPercent(60, 300)
        assertEquals(100, v)
    }

    @Test
    fun rampHalfway() {
        val v = AlarmRamp.computeRampedPercent(40, 150)
        // halfway from 40 to 100 => 70
        assertEquals(70, v)
    }
}

