package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.receiver.AlarmActionReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AwakeLowBandPolicyTest {
    @Test
    fun urgent_active_then_75_keeps_urgent_does_not_start_chime() {
        val current = AlarmActionReceiver.TYPE_LOW // TYPE_LOW represents low/ramp/urgent path
        val start = AwakeLowBandPolicy.shouldStartLowWarnChime(
            currentType = current,
            mgdl = 75.0,
            warnMin = 70.0,
            warnMax = 80.0
        )
        assertFalse(start)
    }

    @Test
    fun ramp_in_progress_then_75_keeps_ramp_does_not_start_chime() {
        val current = AlarmActionReceiver.TYPE_LOW
        val start = AwakeLowBandPolicy.shouldStartLowWarnChime(
            currentType = current,
            mgdl = 75.0,
            warnMin = 70.0,
            warnMax = 80.0
        )
        assertFalse(start)
    }

    @Test
    fun chime_at_75_then_65_should_not_block_escalation_policy_allows() {
        // Policy only governs starting the chime; 65 mg/dL is out of band and should not start a chime.
        // Escalation is governed by AlarmEvaluator, validated separately.
        val start = AwakeLowBandPolicy.shouldStartLowWarnChime(
            currentType = AlarmActionReceiver.TYPE_LOW_WARN,
            mgdl = 65.0,
            warnMin = 70.0,
            warnMax = 80.0
        )
        assertFalse(start)
    }
}

