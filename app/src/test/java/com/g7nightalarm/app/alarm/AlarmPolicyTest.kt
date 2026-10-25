package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.mode.AppMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicyTest {
    @Test
    fun sleepModeUsesFullForLow() {
        val full = AlarmPolicy.shouldUseFullAlarm(AlarmEvent.Low(65.0), AppMode.SLEEP, true)
        assertTrue(full)
    }

    @Test
    fun awakeModeUsesNormalForLow() {
        val full = AlarmPolicy.shouldUseFullAlarm(AlarmEvent.Low(65.0), AppMode.AWAKE, true)
        assertFalse(full)
    }

    @Test
    fun urgentLowAlwaysFull() {
        val full = AlarmPolicy.shouldUseFullAlarm(AlarmEvent.UrgentLow(50.0), AppMode.AWAKE, false)
        assertTrue(full)
    }

    @Test
    fun noDataSafetyNetAfter30Min() {
        val full1 = AlarmPolicy.shouldUseFullAlarm(AlarmEvent.NoData(29), AppMode.AWAKE, true)
        val full2 = AlarmPolicy.shouldUseFullAlarm(AlarmEvent.NoData(30), AppMode.AWAKE, true)
        assertFalse(full1)
        assertTrue(full2)
    }
}

