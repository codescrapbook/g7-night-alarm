package com.g7nightalarm.app.alarm

object AlarmRamp {
    // Linear ramp mapping over totalSeconds (default 300s = 5 min)
    fun computeRampedPercent(startPercent: Int, elapsedSeconds: Int, totalSeconds: Int = 300): Int {
        val s = startPercent.coerceIn(1, 100)
        val frac = (elapsedSeconds.toDouble() / totalSeconds).coerceIn(0.0, 1.0)
        return (s + (100 - s) * frac).toInt().coerceIn(s, 100)
    }
}

