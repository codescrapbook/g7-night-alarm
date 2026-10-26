package com.g7nightalarm.app.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS_NAME = "g7_settings"
val Context.dataStore by preferencesDataStore(SETTINGS_NAME)

enum class Units { MG_DL, MMOL_L }

data class Thresholds(
    // Spec: urgent below 70, low band alert at <=80
    val urgentLow: Double = 70.0,
    val low: Double = 80.0,
    val high: Double = 250.0,
    val rapidFallRateMgdlPerMin: Double = 3.0, // mg/dL per 5 min ~ 0.6/min, this is per min
    val noDataMinutes: Int = 20
)

data class SettingsState(
    val thresholds: Thresholds = Thresholds(),
    val units: Units = Units.MG_DL,
    val allowAlarmSnooze: Boolean = true,
    val snoozeMinutes: Int = 10,
    val sleepMode: Boolean = false,
    val voiceEnabled: Boolean = false,
    val safetyNetAlwaysFull: Boolean = true,
    val autoSleepHour: Int = 23,
    val autoSleepMinute: Int = 0,
    val awakeVolumePercent: Int = 60,
    val awakeLowWarnMin: Double = 70.0,
    val awakeLowWarnMax: Double = 80.0,
    val awakeLowWarnRearmMin: Int = 30,
    val awakeLowBelow70RearmMin: Int = 15,
    val awakeHighRearmMin: Int = 60,
    val awakeSignalLostRearmMin: Int = 30,
    // Music Mode (awake only)
    val musicModeEnabled: Boolean = false,
    val musicLowUri: String? = null,
    val musicHighUri: String? = null,
    val musicSignalUri: String? = null,
    // Monotonic timestamp of last reading arrival (elapsed clock)
    val lastReadingAtMs: Long = 0L,
    val lastLowWarnClearAt: Long = 0L,
    val lastLowClearAt: Long = 0L,
    val lastHighClearAt: Long = 0L,
    val lastSignalClearAt: Long = 0L
)

object SettingsKeys {
    val urgentLow = doublePreferencesKey("urgent_low")
    val low = doublePreferencesKey("low")
    val high = doublePreferencesKey("high")
    val rapidFall = doublePreferencesKey("rapid_fall")
    val noDataMin = intPreferencesKey("no_data_min")
    val units = stringPreferencesKey("units")
    val allowSnooze = booleanPreferencesKey("allow_snooze")
    val snoozeMin = intPreferencesKey("snooze_min")
    val sleepMode = booleanPreferencesKey("sleep_mode")
    val voiceEnabled = booleanPreferencesKey("voice_enabled")
    val safetyNetAlwaysFull = booleanPreferencesKey("safety_net_full")
    val autoSleepHour = intPreferencesKey("auto_sleep_hour")
    val autoSleepMinute = intPreferencesKey("auto_sleep_minute")
    val awakeVolumePercent = intPreferencesKey("awake_volume_percent")
    val awakeLowWarnMin = doublePreferencesKey("awake_low_warn_min")
    val awakeLowWarnMax = doublePreferencesKey("awake_low_warn_max")
    val awakeLowWarnRearmMin = intPreferencesKey("awake_low_warn_rearm_min")
    val awakeLowBelow70RearmMin = intPreferencesKey("awake_low_below70_rearm_min")
    val awakeHighRearmMin = intPreferencesKey("awake_high_rearm_min")
    val awakeSignalLostRearmMin = intPreferencesKey("awake_signal_rearm_min")
    // last clear timestamps
    val lastLowWarnClearAt = longPreferencesKey("last_low_warn_clear_at")
    val lastLowClearAt = longPreferencesKey("last_low_clear_at")
    val lastHighClearAt = longPreferencesKey("last_high_clear_at")
    val lastSignalClearAt = longPreferencesKey("last_signal_clear_at")
    // Music Mode
    val musicModeEnabled = booleanPreferencesKey("music_mode_enabled")
    val musicLowUri = stringPreferencesKey("music_low_uri")
    val musicHighUri = stringPreferencesKey("music_high_uri")
    val musicSignalUri = stringPreferencesKey("music_signal_uri")
    // Monotonic timestamp of last data arrival
    val lastReadingAt = longPreferencesKey("last_reading_at_elapsed")
}

class SettingsRepository(private val context: Context) {
    val flow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            thresholds = Thresholds(
                // Defaults aligned with spec: urgent <70, low band ≤80
                urgentLow = prefs[SettingsKeys.urgentLow] ?: 70.0,
                low = prefs[SettingsKeys.low] ?: 80.0,
                high = prefs[SettingsKeys.high] ?: 250.0,
                rapidFallRateMgdlPerMin = prefs[SettingsKeys.rapidFall] ?: 2.0,
                noDataMinutes = prefs[SettingsKeys.noDataMin] ?: 20
            ),
            units = when (prefs[SettingsKeys.units] ?: Units.MG_DL.name) {
                Units.MMOL_L.name -> Units.MMOL_L
                else -> Units.MG_DL
            },
            allowAlarmSnooze = prefs[SettingsKeys.allowSnooze] ?: true,
            snoozeMinutes = prefs[SettingsKeys.snoozeMin] ?: 10,
            sleepMode = prefs[SettingsKeys.sleepMode] ?: false,
            voiceEnabled = prefs[SettingsKeys.voiceEnabled] ?: false,
            safetyNetAlwaysFull = prefs[SettingsKeys.safetyNetAlwaysFull] ?: true,
            autoSleepHour = prefs[SettingsKeys.autoSleepHour] ?: 23,
            autoSleepMinute = prefs[SettingsKeys.autoSleepMinute] ?: 0,
            awakeVolumePercent = prefs[SettingsKeys.awakeVolumePercent] ?: 60,
            awakeLowWarnMin = prefs[SettingsKeys.awakeLowWarnMin] ?: 70.0,
            awakeLowWarnMax = prefs[SettingsKeys.awakeLowWarnMax] ?: 80.0,
            awakeLowWarnRearmMin = prefs[SettingsKeys.awakeLowWarnRearmMin] ?: 30,
            awakeLowBelow70RearmMin = prefs[SettingsKeys.awakeLowBelow70RearmMin] ?: 15,
            awakeHighRearmMin = prefs[SettingsKeys.awakeHighRearmMin] ?: 60,
            awakeSignalLostRearmMin = prefs[SettingsKeys.awakeSignalLostRearmMin] ?: 30,
            musicModeEnabled = prefs[SettingsKeys.musicModeEnabled] ?: false,
            musicLowUri = prefs[SettingsKeys.musicLowUri],
            musicHighUri = prefs[SettingsKeys.musicHighUri],
            musicSignalUri = prefs[SettingsKeys.musicSignalUri],
            lastReadingAtMs = prefs[SettingsKeys.lastReadingAt] ?: 0L,
            lastLowWarnClearAt = prefs[SettingsKeys.lastLowWarnClearAt] ?: 0L,
            lastLowClearAt = prefs[SettingsKeys.lastLowClearAt] ?: 0L,
            lastHighClearAt = prefs[SettingsKeys.lastHighClearAt] ?: 0L,
            lastSignalClearAt = prefs[SettingsKeys.lastSignalClearAt] ?: 0L
        )
    }

    suspend fun update(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs ->
            block(prefs)
        }
    }
}

