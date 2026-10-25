package com.g7nightalarm.app.schedule

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SleepSchedulerTest {
    @Test
    fun nextTriggerIsFutureOrTomorrow() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hourNow = cal.get(Calendar.HOUR_OF_DAY)
        val minuteNow = cal.get(Calendar.MINUTE)

        val sameMinute = SleepScheduler.computeNextTrigger(now, hourNow, minuteNow)
        assertTrue(sameMinute >= now)

        val past = SleepScheduler.computeNextTrigger(now, (hourNow + 23) % 24, minuteNow)
        assertTrue(past > now)
    }
}

