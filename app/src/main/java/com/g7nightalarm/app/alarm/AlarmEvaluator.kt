package com.g7nightalarm.app.alarm

import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsState
import kotlin.math.abs
import com.g7nightalarm.app.time.ClockHolder

sealed class AlarmEvent {
    data class UrgentLow(val valueMgdl: Double) : AlarmEvent()
    data class Low(val valueMgdl: Double) : AlarmEvent()
    data class High(val valueMgdl: Double) : AlarmEvent()
    data class RapidFall(val rateMgdlPerMin: Double) : AlarmEvent()
    data class NoData(val minutes: Int) : AlarmEvent()
    data object None : AlarmEvent()
}

class AlarmEvaluator {
    fun evaluate(
        latest: GlucoseReading?,
        previous: GlucoseReading?,
        nowMs: Long,
        settings: SettingsState
    ): AlarmEvent {
        if (latest == null) return AlarmEvent.NoData(settings.thresholds.noDataMinutes)

        val lastAt = settings.lastReadingAtMs
        val ageMin = if (lastAt > 0L) {
            // Monotonic basis: nowMs and lastAt are both elapsed-time based
            ((nowMs - lastAt) / 60000.0).toInt()
        } else {
            // Fallback: both values should share the same basis (caller provides nowMs accordingly)
            ((nowMs - latest.timestamp) / 60000.0).toInt()
        }
        if (ageMin >= settings.thresholds.noDataMinutes) {
            return AlarmEvent.NoData(ageMin)
        }

        val mgdl = latest.mgdl
        if (mgdl <= settings.thresholds.urgentLow) {
            return AlarmEvent.UrgentLow(mgdl)
        }
        if (mgdl <= settings.thresholds.low) {
            return AlarmEvent.Low(mgdl)
        }
        if (mgdl >= settings.thresholds.high) {
            return AlarmEvent.High(mgdl)
        }

        val rate = latest.slopeMgdlPerMin ?: previous?.let { prev ->
            val dtMin = (latest.timestamp - prev.timestamp) / 60000.0
            if (dtMin > 0) (latest.mgdl - prev.mgdl) / dtMin else null
        }
        if (rate != null && rate < -abs(settings.thresholds.rapidFallRateMgdlPerMin)) {
            return AlarmEvent.RapidFall(rate)
        }
        return AlarmEvent.None
    }
}

