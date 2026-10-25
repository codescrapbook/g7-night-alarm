package com.g7nightalarm.app.debug

import android.content.Context
import android.util.Log
import com.g7nightalarm.app.BuildConfig
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

object DebugData {
    private var job: Job? = null

    fun start(context: Context) {
        if (!BuildConfig.DEBUG) return
        if (job != null) return
        val db = AppDatabase.get(context)
        job = CoroutineScope(Dispatchers.IO).launch {
            var value = 130.0
            var dir = -0.5
            while (isActive) {
                try {
                    val ts = System.currentTimeMillis()
                    value += dir * Random.nextDouble(0.0, 3.0)
                    if (value < 60) dir = 0.8
                    if (value > 260) dir = -0.8
                    val r = GlucoseReading(
                        timestamp = ts,
                        mgdl = value.coerceIn(40.0, 300.0),
                        slopeMgdlPerMin = dir,
                        slopeName = if (dir < -0.3) "SingleDown" else if (dir > 0.3) "SingleUp" else "Flat",
                        source = "debug"
                    )
                    db.glucoseDao().insert(r)
                } catch (t: Throwable) {
                    Log.e("DebugData", "Error generating debug reading", t)
                }
                delay(60_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

