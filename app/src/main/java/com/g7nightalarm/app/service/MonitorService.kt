package com.g7nightalarm.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.g7nightalarm.app.G7NightAlarmApp
import com.g7nightalarm.app.R
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.alarm.AlarmEvaluator
import com.g7nightalarm.app.alarm.AlarmEvent
import com.g7nightalarm.app.alarm.SnoozeController
import com.g7nightalarm.app.alarm.SnoozePolicy
import com.g7nightalarm.app.schedule.SnoozeScheduler
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.ui.MainActivity
import com.g7nightalarm.app.voice.TtsManager
import com.g7nightalarm.app.voice.VoiceKeywordListener
import com.g7nightalarm.app.voice.VoiceModelManager
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import com.g7nightalarm.app.time.ClockHolder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import com.g7nightalarm.app.net.Parsing
import com.g7nightalarm.app.ui.trend.deriveTrend
import com.g7nightalarm.app.ui.trend.symbol

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
    // Runtime receiver to ensure implicit xDrip broadcasts are received on API 26+
    private val xdripReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            try {
                com.g7nightalarm.app.receiver.XdripBroadcastReceiver().onReceive(this@MonitorService, intent)
            } catch (_: Throwable) {}
        }
    }
    private val bgUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_BG_UPDATED) {
                scope.launch {
                    try {
                        val readings = db.glucoseDao().latest2()
                        val latest = readings.getOrNull(0)
                        val prev = readings.getOrNull(1)
                        val s = settings.flow.first()
                        val evalNow = if (s.lastReadingAtMs > 0L) ClockHolder.nowMs() else System.currentTimeMillis()
                        val event = evaluator.evaluate(latest, prev, evalNow, s)
                        handleAlarmEvent(event, latest)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }
    @Volatile private var alarmActive: Boolean = false
    @Volatile private var awakeAlarmType: String? = null
    private val alarmStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ALARM_STOPPED) {
                alarmActive = false
                // Reset awake alarm type so re-arming after a window works,
                // and in-progress ramps are not considered active after CLEAR/dismiss.
                awakeAlarmType = null
                voice?.resume()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(Dispatchers.Default)
        db = AppDatabase.get(this)
        settings = SettingsRepository(this)
        modeRepo = ModeRepository(this)
        alarmController = AlarmController(this)
        tts = TtsManager(this)
        tts.ensure()
        // Record monitoring start on first boot so NoData staleness uses a real baseline
        scope.launch {
            try {
                val s = settings.flow.first()
                if (s.monitoringStartedAtMs <= 0L) {
                    settings.update { p -> p[com.g7nightalarm.app.settings.SettingsKeys.monitoringStartedAt] = ClockHolder.nowMs() }
                }
                SnoozeScheduler.rescheduleIfPending(this@MonitorService, ClockHolder.nowWallMs(), s.snoozeUntilWallMs)
            } catch (_: Throwable) {}
        }

        startForeground(MONITOR_NOTIF_ID, buildMonitorNotification(null))

        observeAndEvaluate()
        startFallbackPolling()
        observeVoiceToggle()
        scheduleAutoSleep()
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(alarmStateReceiver, IntentFilter(ACTION_ALARM_STOPPED), Context.RECEIVER_NOT_EXPORTED)
            // Receive xDrip+ implicit broadcasts even on newer Android where manifest implicit broadcasts are limited
            registerReceiver(xdripReceiver, IntentFilter(com.g7nightalarm.app.receiver.XdripBroadcastReceiver.ACTION_BG_ESTIMATE), Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(xdripReceiver, IntentFilter(com.g7nightalarm.app.receiver.XdripBroadcastReceiver.ACTION_BG_NO_DATA), Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(bgUpdateReceiver, IntentFilter(ACTION_BG_UPDATED), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(alarmStateReceiver, IntentFilter(ACTION_ALARM_STOPPED))
            @Suppress("DEPRECATION")
            registerReceiver(xdripReceiver, IntentFilter(com.g7nightalarm.app.receiver.XdripBroadcastReceiver.ACTION_BG_ESTIMATE))
            @Suppress("DEPRECATION")
            registerReceiver(xdripReceiver, IntentFilter(com.g7nightalarm.app.receiver.XdripBroadcastReceiver.ACTION_BG_NO_DATA))
            @Suppress("DEPRECATION")
            registerReceiver(bgUpdateReceiver, IntentFilter(ACTION_BG_UPDATED))
        }
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
        val sUnits = kotlinx.coroutines.runBlocking { settings.flow.first().units }
        val content = latest?.let {
            val ageMin = ((System.currentTimeMillis() - it.timestamp) / 60000.0).toInt()
            val slope = deriveTrend(it, previous = null).symbol()
            val valueStr = if (sUnits == com.g7nightalarm.app.settings.Units.MG_DL) format(it.mgdl)
            else mmolFormat.format(it.mgdl / 18.0)
            val unitStr = if (sUnits == com.g7nightalarm.app.settings.Units.MG_DL) "mg/dL" else "mmol/L"
            "$valueStr $unitStr  $slope  · ${ageMin}m"
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
                    val evalNow = if (s.lastReadingAtMs > 0L) ClockHolder.nowMs() else System.currentTimeMillis()
                    val event = evaluator.evaluate(latest, prev, evalNow, s)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(MONITOR_NOTIF_ID, buildMonitorNotification(latest))
                    handleAlarmEvent(event, latest)
                }
        }
    }

    private fun handleAlarmEvent(event: AlarmEvent, latest: GlucoseReading?) {
        scope.launch {
            val s = settings.flow.first()
            var bypassQuiet = false
            if (s.snoozeUntilWallMs > 0L) {
                val signalLost = event is AlarmEvent.NoData &&
                    event.minutes >= s.thresholds.noDataMinutes
                when (SnoozePolicy.duringSnooze(
                    nowWallMs = ClockHolder.nowWallMs(),
                    untilWallMs = s.snoozeUntilWallMs,
                    snoozedType = s.snoozedAlarmType,
                    snoozedMgdl = s.snoozedMgdl,
                    currentMgdl = latest?.mgdl,
                    signalLost = signalLost,
                    urgentLowThreshold = s.thresholds.urgentLow
                )) {
                    SnoozePolicy.DuringSnooze.RearmDue -> {
                        SnoozeController.fire(this@MonitorService, force = true)
                        return@launch
                    }
                    SnoozePolicy.DuringSnooze.StayQuiet -> return@launch
                    SnoozePolicy.DuringSnooze.BreakNow -> {
                        SnoozeController.cancel(this@MonitorService)
                        bypassQuiet = true
                    }
                }
            }
            val mode = if (s.sleepMode) AppMode.SLEEP else AppMode.AWAKE
            if (mode == AppMode.SLEEP) {
                val full = com.g7nightalarm.app.alarm.AlarmPolicy.shouldUseFullAlarm(event, mode, s.safetyNetAlwaysFull)
                if (full) {
                    alarmActive = true
                    voice?.pause()
                    val uiType = com.g7nightalarm.app.alarm.AlarmUiTypeMapper.map(event)
                    if (uiType != null) {
                        alarmController.startAlarm(type = uiType)
                    }
                }
                return@launch
            }
            try { android.util.Log.i("MonitorService", "handleAlarmEvent type=${event::class.simpleName} latestMgdl=${latest?.mgdl} awakeType=$awakeAlarmType") } catch (_: Throwable) {}
            // Awake mode behavior with band and snooze
            val now = ClockHolder.nowMs()
            fun quietSuppressed(lastClear: Long, windowMin: Int): Boolean {
                if (bypassQuiet) return false
                return com.g7nightalarm.app.alarm.AlarmPolicy.shouldSuppressBySnooze(now, lastClear, windowMin)
            }
            val awakeVol = s.awakeVolumePercent
            val musicEnabled = s.musicModeEnabled
            val lowUri = s.musicLowUri?.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
            val highUri = s.musicHighUri?.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
            val sigUri = s.musicSignalUri?.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
            // Low warning band check using latest
            val lwMin = s.awakeLowWarnMin
            val lwMax = s.awakeLowWarnMax
            if (latest != null && latest.mgdl in lwMin..lwMax) {
                val suppressed = quietSuppressed(s.lastLowWarnClearAt, s.awakeLowWarnRearmMin)
                try { android.util.Log.i("MonitorService", "LOW_WARN band mgdl=${latest.mgdl} suppressed=$suppressed now=$now lastLowWarnClearAt=${s.lastLowWarnClearAt} type=$awakeAlarmType") } catch (_: Throwable) {}
                if (suppressed) {
                    return@launch
                }
                // Start low-warning chime when allowed by priority policy:
                // urgent/ramp > low-warn > high > signal-lost. Do not downgrade over LOW/LOW_WARN.
                if (!com.g7nightalarm.app.alarm.AlarmController.isRampActive() &&
                    com.g7nightalarm.app.alarm.AwakeLowBandPolicy.shouldStartLowWarnChime(
                        currentType = awakeAlarmType,
                        mgdl = latest.mgdl,
                        warnMin = lwMin,
                        warnMax = lwMax
                    )
                ) {
                    startAwakeNotification(
                        AlarmActionReceiver.TYPE_LOW_WARN,
                        "Low warning",
                        "Glucose ${latest.mgdl.toInt()} mg/dL in warning band"
                    )
                    alarmController.startAwakeChimeLowWarning(awakeVol)
                    awakeAlarmType = AlarmActionReceiver.TYPE_LOW_WARN
                }
                return@launch
            }
            when (event) {
                is AlarmEvent.UrgentLow -> {
                    val suppressed = quietSuppressed(s.lastLowClearAt, s.awakeLowBelow70RearmMin)
                    try { android.util.Log.i("MonitorService", "URGENT_LOW mgdl=${event.valueMgdl} suppressed=$suppressed now=$now lastLowClearAt=${s.lastLowClearAt} type=$awakeAlarmType") } catch (_: Throwable) {}
                    if (suppressed) return@launch
                    if (com.g7nightalarm.app.alarm.AlarmPreemption.shouldStartLowRamp(awakeAlarmType)) {
                        try { android.util.Log.d("MonitorService", "START LOW RAMP urgent=true typeWas=$awakeAlarmType vol=$awakeVol") } catch (_: Throwable) {}
                        startAwakeNotification(AlarmActionReceiver.TYPE_LOW, "Low (urgent)", "Glucose ${event.valueMgdl.toInt()} mg/dL — escalating")
                        if (musicEnabled) {
                            alarmController.startAwakeMusicRampLow(awakeVol, urgent = true, contentUri = lowUri)
                        } else {
                            alarmController.startAwakeRampLow(awakeVol, urgent = true)
                        }
                        awakeAlarmType = AlarmActionReceiver.TYPE_LOW
                    }
                }
                is AlarmEvent.Low -> {
                    val suppressed = quietSuppressed(s.lastLowClearAt, s.awakeLowBelow70RearmMin)
                    try { android.util.Log.i("MonitorService", "LOW mgdl=${event.valueMgdl} suppressed=$suppressed now=$now lastLowClearAt=${s.lastLowClearAt} type=$awakeAlarmType") } catch (_: Throwable) {}
                    if (suppressed) return@launch
                    if (com.g7nightalarm.app.alarm.AlarmPreemption.shouldStartLowRamp(awakeAlarmType)) {
                        try { android.util.Log.d("MonitorService", "START LOW RAMP urgent=false typeWas=$awakeAlarmType vol=$awakeVol") } catch (_: Throwable) {}
                        startAwakeNotification(AlarmActionReceiver.TYPE_LOW, "Low", "Glucose ${event.valueMgdl.toInt()} mg/dL — escalating")
                        if (musicEnabled) {
                            alarmController.startAwakeMusicRampLow(awakeVol, urgent = false, contentUri = lowUri)
                        } else {
                            alarmController.startAwakeRampLow(awakeVol, urgent = false)
                        }
                        awakeAlarmType = AlarmActionReceiver.TYPE_LOW
                    }
                }
                is AlarmEvent.High -> {
                    if (quietSuppressed(s.lastHighClearAt, s.awakeHighRearmMin)) return@launch
                    startAwakeNotification(AlarmActionReceiver.TYPE_HIGH, "High", "Glucose high")
                    if (musicEnabled) {
                        if (highUri != null) {
                            alarmController.startAwakeMusicFromUri(awakeVol, highUri)
                        } else {
                            alarmController.startAwakeMusicHighDefault(awakeVol)
                        }
                    } else {
                        alarmController.startAwakeChimeHigh(awakeVol)
                    }
                    awakeAlarmType = AlarmActionReceiver.TYPE_HIGH
                }
                is AlarmEvent.NoData -> {
                    if (event.minutes >= 30) {
                        try {
                            android.util.Log.i(
                                "MonitorService",
                                "NO_DATA minutes=${event.minutes} now=$now lastSignalClearAt=${s.lastSignalClearAt} music=$musicEnabled awakeVol=$awakeVol"
                            )
                        } catch (_: Throwable) {}
                        if (quietSuppressed(s.lastSignalClearAt, s.awakeSignalLostRearmMin)) return@launch
                        startAwakeNotification(AlarmActionReceiver.TYPE_SIGNAL, "Signal lost", "No data for ${event.minutes} minutes")
                        if (musicEnabled) {
                            if (sigUri != null) {
                                alarmController.startAwakeMusicFromUri(awakeVol, sigUri)
                            } else {
                                alarmController.startAwakeMusicSignalDefault(awakeVol)
                            }
                        } else {
                            alarmController.startAwakeChimeSignal(awakeVol)
                        }
                        awakeAlarmType = AlarmActionReceiver.TYPE_SIGNAL
                    }
                }
                else -> {}
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
                                val parsed = Parsing.parseLatestSgv(body)
                                if (parsed != null) {
                                    val reading = GlucoseReading(
                                        timestamp = parsed.date,
                                        mgdl = parsed.sgv,
                                        slopeMgdlPerMin = null,
                                        slopeName = null,
                                        source = "xdrip_http"
                                    )
                                    db.glucoseDao().insert(reading)
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
        unregisterReceiver(alarmStateReceiver)
        try { unregisterReceiver(bgUpdateReceiver) } catch (_: Throwable) {}
        try { unregisterReceiver(xdripReceiver) } catch (_: Throwable) {}
        voice?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val MONITOR_NOTIF_ID = 42
        const val ACTION_BG_UPDATED = "com.g7nightalarm.BG_UPDATED"
        private val mgdlFormat = DecimalFormat("###0.#")
        private val mmolFormat = DecimalFormat("##0.0")
        private fun format(v: Double) = mgdlFormat.format(v)
        const val ACTION_ALARM_STOPPED = "com.g7nightalarm.ALARM_STOPPED"
        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun startAwakeNotification(type: String, title: String, text: String) {
        val clearIntent = Intent(AlarmActionReceiver.ACTION_CLEAR).apply {
            setClass(this@MonitorService, AlarmActionReceiver::class.java)
            putExtra(AlarmActionReceiver.EXTRA_TYPE, type)
        }
        val clearPi = PendingIntent.getBroadcast(
            this, 200 + type.hashCode(),
            clearIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            MONITOR_NOTIF_ID + 2,
            NotificationCompat.Builder(this, G7NightAlarmApp.Channels.MONITOR_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .addAction(0, getString(R.string.action_dismiss), clearPi)
                .build()
        )
    }

    private fun observeVoiceToggle() {
        scope.launch {
            settings.flow.collect { s ->
                if (s.voiceEnabled) {
                    // Ensure model installed, with progress notification
                    if (!VoiceModelManager.hasModel(this@MonitorService)) {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val notifId = MONITOR_NOTIF_ID + 1
                        nm.notify(
                            notifId,
                            NotificationCompat.Builder(this@MonitorService, G7NightAlarmApp.Channels.MONITOR_ID)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setOngoing(true)
                                .setContentTitle("Downloading voice model…")
                                .setProgress(100, 0, true)
                                .build()
                        )
                        val ok = VoiceModelManager.ensureModelInstalled(this@MonitorService) { pct ->
                            nm.notify(
                                notifId,
                                NotificationCompat.Builder(this@MonitorService, G7NightAlarmApp.Channels.MONITOR_ID)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setOngoing(true)
                                    .setContentTitle("Downloading voice model…")
                                    .setProgress(100, pct.coerceIn(0, 100), false)
                                    .build()
                            )
                        }
                        nm.cancel(notifId)
                        if (!ok) {
                            tts.speak("Voice model download failed. Will retry later.", 60)
                            return@collect
                        } else {
                            tts.speak("Voice control enabled.", 60)
                        }
                    }
                    if (!alarmActive) {
                        if (voice == null) voice = VoiceKeywordListener(this@MonitorService, modeRepo, tts)
                        voice?.startIfEnabledAndReady(true)
                    } else {
                        voice?.pause()
                    }
                } else {
                    voice?.pause()
                }
            }
        }
    }

    private fun scheduleAutoSleep() {
        scope.launch {
            val s = settings.flow.first()
            com.g7nightalarm.app.schedule.SleepScheduler.schedule(
                this@MonitorService, s.autoSleepHour, s.autoSleepMinute
            )
        }
    }

}

