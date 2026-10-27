package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.settings.SettingsState
import com.g7nightalarm.app.settings.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmEvaluatorColdStartTest {
    private val evaluator = AlarmEvaluator()

    @Test
    fun coldStart_beforeThreshold_isNone() {
        val now = 1_000_000L
        val s = SettingsState(
            thresholds = Thresholds(noDataMinutes = 30),
            lastReadingAtMs = 0L,
            monitoringStartedAtMs = now - 10 * 60_000L // 10 minutes ago
        )
        val event = evaluator.evaluate(latest = null, previous = null, nowMs = now, settings = s)
        assertEquals(AlarmEvent.None, event)
    }

    @Test
    fun coldStart_afterThreshold_isNoData() {
        val now = 2_000_000L
        val s = SettingsState(
            thresholds = Thresholds(noDataMinutes = 30),
            lastReadingAtMs = 0L,
            monitoringStartedAtMs = now - 35 * 60_000L // 35 minutes ago
        )
        val event = evaluator.evaluate(latest = null, previous = null, nowMs = now, settings = s)
        require(event is AlarmEvent.NoData)
        assertEquals(true, event.minutes >= 30)
    }

    @Test
    fun normalStaleness_unchanged_usesLastReadingAt() {
        val now = 3_000_000L
        val s = SettingsState(
            thresholds = Thresholds(noDataMinutes = 30),
            lastReadingAtMs = now - 45 * 60_000L,
            monitoringStartedAtMs = now - 60 * 60_000L
        )
        val event = evaluator.evaluate(latest = null, previous = null, nowMs = now, settings = s)
        require(event is AlarmEvent.NoData)
        assertEquals(true, event.minutes >= 45)
    }
}

