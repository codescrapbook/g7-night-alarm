package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPreemptionTest {
    @Test
    fun high_ringing_then_65_starts_low_ramp() {
        val ok = AlarmPreemption.shouldStartLowRamp(AlarmActionReceiver.TYPE_HIGH)
        assertTrue(ok)
    }

    @Test
    fun low_already_active_then_65_does_not_restart_ramp() {
        val ok = AlarmPreemption.shouldStartLowRamp(AlarmActionReceiver.TYPE_LOW)
        assertFalse(ok)
    }
}

