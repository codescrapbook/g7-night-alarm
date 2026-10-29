package com.g7nightalarm.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeRampTest {
    @Test
    fun ramp_is_monotonic_and_hits_max_at_5min() {
        val max = 7
        val start = 3
        val total = 5 * 60 * 1000
        var prev = volumeAt(0, total, start, max)
        assertEquals(start, prev)
        val mid = volumeAt(150_000, total, start, max)
        assertTrue("mid should be >= start", mid >= start)
        val late = volumeAt(299_000, total, start, max)
        assertTrue("late >= mid", late >= mid)
        val full = volumeAt(300_000, total, start, max)
        assertEquals(max, full)
        val after = volumeAt(600_000, total, start, max)
        assertEquals(max, after)
    }
}

