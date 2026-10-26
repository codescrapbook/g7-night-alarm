package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.round
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.SettingsKeys
import com.g7nightalarm.app.time.ClockHolder

class XdripBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BG_ESTIMATE -> handleEstimate(context, intent)
            ACTION_BG_NO_DATA -> handleNoData(context)
        }
    }

    private fun handleEstimate(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val mgdl = bundle.getDouble(EXTRA_BG_ESTIMATE, Double.NaN)
        val slopePerMs = if (bundle.containsKey(EXTRA_BG_SLOPE)) {
            bundle.getDouble(EXTRA_BG_SLOPE)
        } else null
        val slopePerMin = slopePerMs?.let { it * 1000.0 * 60.0 }
        val slopeName = bundle.getString(EXTRA_BG_SLOPE_NAME)
        val ts = bundle.getLong(EXTRA_TIMESTAMP, System.currentTimeMillis())

        if (mgdl.isNaN()) {
            Log.w(TAG, "Received BgEstimate but value was NaN; extras=$bundle")
            return
        }
        val reading = GlucoseReading(
            timestamp = ts,
            mgdl = round(mgdl * 10.0) / 10.0,
            slopeMgdlPerMin = slopePerMin,
            slopeName = slopeName,
            source = "xdrip_broadcast"
        )
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(context).glucoseDao()
            dao.insert(reading)
            dao.prune(System.currentTimeMillis() - HISTORY_PRUNE_MS)
            // Record last reading arrival using the monotonic debug-aware clock
            val repo = SettingsRepository(context)
            repo.update { p -> p[SettingsKeys.lastReadingAt] = ClockHolder.nowMs() }
            try {
                android.util.Log.d(
                    TAG,
                    "BG_ESTIMATE received mgdl=$mgdl ts=$ts slopePerMin=$slopePerMin updatedLastAt=${ClockHolder.nowMs()}"
                )
            } catch (_: Throwable) {}
        }
    }

    private fun handleNoData(context: Context) {
        // Could set a flag or let service detect staleness
        Log.w(TAG, "xDrip+ reported BgEstimateNoData")
    }

    companion object {
        private const val TAG = "XdripReceiver"
        private const val HISTORY_PRUNE_MS = 14L * 24L * 60L * 60L * 1000L

        const val ACTION_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate"
        const val ACTION_BG_NO_DATA = "com.eveningoutpost.dexdrip.BgEstimateNoData"

        const val EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate"
        const val EXTRA_BG_SLOPE = "com.eveningoutpost.dexdrip.Extras.BgSlope"
        const val EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName"
        const val EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery"
        const val EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time"
        const val EXTRA_RAW = "com.eveningoutpost.dexdrip.Extras.Raw"
    }
}

