package com.g7nightalarm.app.alarm

/**
 * Pure function for computing the target alarm STREAM_ALARM volume given elapsed time.
 * Returns an integer volume in [1, maxVol], monotonically increasing and capped at maxVol.
 */
fun volumeAt(elapsedMs: Long, totalMs: Int, startVol: Int, maxVol: Int): Int {
    if (maxVol <= 0) return 0
    if (elapsedMs <= 0L) return startVol.coerceIn(1, maxVol)
    val frac = (elapsedMs.toDouble() / totalMs).coerceIn(0.0, 1.0)
    val startPct = startVol.toDouble() / maxVol.toDouble()
    val endPct = 1.0
    val curPct = startPct + (endPct - startPct) * frac
    val set = (curPct * maxVol).toInt()
    return set.coerceIn(1, maxVol)
}

