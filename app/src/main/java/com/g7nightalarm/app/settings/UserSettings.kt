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
    val urgentLow: Double = 55.0,
    val low: Double = 70.0,
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
    val autoSleepMinute: Int = 0
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
}

class SettingsRepository(private val context: Context) {
    val flow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            thresholds = Thresholds(
                urgentLow = prefs[SettingsKeys.urgentLow] ?: 55.0,
                low = prefs[SettingsKeys.low] ?: 70.0,
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
            autoSleepMinute = prefs[SettingsKeys.autoSleepMinute] ?: 0
        )
    }

    suspend fun update(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs ->
            block(prefs)
        }
    }
}

