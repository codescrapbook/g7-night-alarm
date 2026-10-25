package com.g7nightalarm.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.g7nightalarm.app.G7NightAlarmApp
import com.g7nightalarm.app.R
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.alarm.AlarmEvaluator
import com.g7nightalarm.app.alarm.AlarmEvent
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.ui.MainActivity
import com.g7nightalarm.app.voice.TtsManager
import com.g7nightalarm.app.voice.VoiceKeywordListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class MonitorService : Service() {

    private lateinit var scope: CoroutineScope
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private val evaluator = AlarmEvaluator()
    private lateinit var alarmController: AlarmController
    private var pollingJob: Job? = null
    private lateinit var modeRepo: ModeRepository
    private lateinit var tts: TtsManager
    private var voice: VoiceKeywordListener? = null
    @Volatile private var alarmActive: Boolean = false

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(Dispatchers.Default)
        db = AppDatabase.get(this)
        settings = SettingsRepository(this)
        modeRepo = ModeRepository(this)
        alarmController = AlarmController(this)
        tts = TtsManager(this)
        tts.ensure()

        startForeground(MONITOR_NOTIF_ID, buildMonitorNotification(null))

        observeAndEvaluate()
        startFallbackPolling()
        observeVoiceToggle()
        scheduleAutoSleep()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Update notification immediately
        scope.launch {
            val latest = db.glucoseDao().latest()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(MONITOR_NOTIF_ID, buildMonitorNotification(latest))
        }
        return START_STICKY
    }

    private fun buildMonitorNotification(latest: GlucoseReading?): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = latest?.let {
            val ageMin = ((System.currentTimeMillis() - it.timestamp) / 60000.0).toInt()
            val slope = it.slopeName ?: ""
            "${format(it.mgdl)} mg/dL  $slope  · ${ageMin}m"
        } ?: "Waiting for data…"

        return NotificationCompat.Builder(this, G7NightAlarmApp.Channels.MONITOR_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("G7 Night Alarm")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun observeAndEvaluate() {
        val latestFlow = db.glucoseDao().latestFlow()
        val prevFlow = flow {
            var last: GlucoseReading? = null
            latestFlow.collect { cur ->
                emit(last)
                last = cur
            }
        }
        val settingsFlow = settings.flow
        scope.launch {
            combine(latestFlow, prevFlow, settingsFlow) { latest, prev, s -> Triple(latest, prev, s) }
                .distinctUntilChanged()
                .collect { (latest, prev, s) ->
                    val event = evaluator.evaluate(latest, prev, System.currentTimeMillis(), s)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(MONITOR_NOTIF_ID, buildMonitorNotification(latest))
                    handleAlarmEvent(event, latest)
                }
        }
    }

    private fun handleAlarmEvent(event: AlarmEvent, latest: GlucoseReading?) {
        scope.launch {
            val s = kotlinx.coroutines.flow.first(settings.flow)
            val mode = if (s.sleepMode) AppMode.SLEEP else AppMode.AWAKE
            val full = when (event) {
                is AlarmEvent.None -> false
                is AlarmEvent.UrgentLow -> true
                is AlarmEvent.NoData -> {
                    if (s.safetyNetAlwaysFull && event.minutes >= 30) true
                    else mode == AppMode.SLEEP
                }
                else -> mode == AppMode.SLEEP
            }
            if (full) {
                alarmActive = true
                voice?.pause()
                alarmController.startAlarm()
            } else if (event !is AlarmEvent.None) {
                // Normal mode notification / TTS
                val phrase = when (event) {
                    is AlarmEvent.Low -> "Glucose low"
                    is AlarmEvent.High -> "Glucose high"
                    is AlarmEvent.RapidFall -> "Glucose falling rapidly"
                    is AlarmEvent.NoData -> "No data received"
                    else -> ""
                }
                if (phrase.isNotEmpty()) {
                    tts.speak(phrase, 60)
                }
            }
        }
    }

    private fun startFallbackPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
            while (true) {
                try {
                    val req = Request.Builder()
                        .url("http://127.0.0.1:17580/sgv.json")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val arr = JSONArray(body)
                                if (arr.length() > 0) {
                                    val obj = arr.getJSONObject(0)
                                    val sgv = obj.optDouble("sgv", Double.NaN)
                                    val date = obj.optLong("date", System.currentTimeMillis())
                                    if (!sgv.isNaN()) {
                                        val reading = GlucoseReading(
                                            timestamp = date,
                                            mgdl = sgv,
                                            slopeMgdlPerMin = null,
                                            slopeName = null,
                                            source = "xdrip_http"
                                        )
                                        db.glucoseDao().insert(reading)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // ignore; server may be disabled
                }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        voice?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val MONITOR_NOTIF_ID = 42
        private val mgdlFormat = DecimalFormat("###0.#")
        private fun format(v: Double) = mgdlFormat.format(v)
        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun observeVoiceToggle() {
        scope.launch {
            settings.flow.collect { s ->
                if (s.voiceEnabled && !alarmActive) {
                    if (voice == null) voice = VoiceKeywordListener(this@MonitorService, modeRepo, tts)
                    voice?.startIfEnabledAndReady(true)
                } else {
                    voice?.pause()
                }
            }
        }
    }

    private fun scheduleAutoSleep() {
        scope.launch {
            val s = kotlinx.coroutines.flow.first(settings.flow)
            com.g7nightalarm.app.schedule.SleepScheduler.schedule(
                this@MonitorService, s.autoSleepHour, s.autoSleepMinute
            )
        }
    }
}

