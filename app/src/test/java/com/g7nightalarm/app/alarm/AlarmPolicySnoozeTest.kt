package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.mode.AppMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicySnoozeTest {
    @Test
    fun suppressWithinWindow() {
        val now = 1_000_000L
        val last = now - 10 * 60_000L
        assertTrue(AlarmPolicy.shouldSuppressBySnooze(now, last, 30))
    }

    @Test
    fun allowAfterWindow() {
        val now = 1_000_000L
        val last = now - 31 * 60_000L
        assertFalse(AlarmPolicy.shouldSuppressBySnooze(now, last, 30))
    }
}

