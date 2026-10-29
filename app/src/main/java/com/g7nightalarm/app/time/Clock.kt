package com.g7nightalarm.app.time

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Monotonic clock for timing decisions (ramp, snooze windows, staleness).
 * Default uses SystemClock.elapsedRealtime() and is thread-safe.
 */
interface Clock {
    fun nowMs(): Long
}

/**
 * Global clock holder. Production uses elapsed realtime.
 * In debug builds a receiver may adjust [debugOffsetMs] to simulate time.
 */
object ClockHolder : Clock {
    // Debug offset applied to elapsedRealtime; zero in production.
    private val debugOffsetMs = AtomicLong(0L)
    // Debug offset applied to the wall clock used by snooze alarms. Zero in production.
    private val debugWallOffsetMs = AtomicLong(0L)

    fun setDebugOffsetMs(offsetMs: Long) {
        debugOffsetMs.set(offsetMs)
    }

    fun addDebugOffsetMs(deltaMs: Long) {
        debugOffsetMs.addAndGet(deltaMs)
    }

    fun addDebugWallOffsetMs(deltaMs: Long) {
        debugWallOffsetMs.addAndGet(deltaMs)
    }

    fun clearDebugOffset() {
        debugOffsetMs.set(0L)
        debugWallOffsetMs.set(0L)
    }

    override fun nowMs(): Long {
        return SystemClock.elapsedRealtime() + debugOffsetMs.get()
    }

    /** Wall clock for snooze wake-ups. Tests advance it with the fake clock. */
    fun nowWallMs(): Long = System.currentTimeMillis() + debugWallOffsetMs.get()
}

