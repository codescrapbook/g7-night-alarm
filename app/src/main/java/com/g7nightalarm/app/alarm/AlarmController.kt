package com.g7nightalarm.app.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.g7nightalarm.app.G7NightAlarmApp
import com.g7nightalarm.app.R
import com.g7nightalarm.app.ui.alarm.AlarmActivity
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin

class AlarmController(private val context: Context) {
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private var previousAlarmVolume: Int? = null
    companion object {
        @Volatile private var globalPreviousAlarmVolume: Int? = null
        private const val ALARM_NOTIF_ID = 1001
        @Volatile var testNowMs: Long? = null
    }
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var rampJob: Thread? = null
    private var mediaPlayer: MediaPlayer? = null
    @Volatile var isMusicPlaying: Boolean = false
    /**
     * Time source used for testing the awake-mode volume ramp. Defaults to System.currentTimeMillis.
     * Tests may override this to simulate passage of time deterministically.
     */
    var nowProvider: () -> Long = { testNowMs ?: System.currentTimeMillis() }

    fun showFullScreenAlarmNotification(): Notification {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, G7NightAlarmApp.Channels.ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("URGENT Glucose Alarm")
            .setContentText("Tap to open and dismiss")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
    }

    fun startAlarm() {
        acquireWakeLock()
        forceAlarmVolumeMax()
        startVibration()
        startAudio()
        // show a heads-up full-screen notification to surface the activity
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALARM_NOTIF_ID, showFullScreenAlarmNotification())
        // also start the activity proactively
        context.startActivity(
            Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    fun stopAlarm() {
        stopAudio()
        stopVibration()
        releaseWakeLock()
        restoreAlarmVolume()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ALARM_NOTIF_ID)
    }

    private fun acquireWakeLock() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock?.isHeld != true) {
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "G7NightAlarm:AlarmWakeLock"
            ).apply { setReferenceCounted(false); acquire(10 * 60 * 1000L) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), intArrayOf(255, 255, 0), 0)
        } else null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 500), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun forceAlarmVolumeMax() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
        previousAlarmVolume = cur
        if (globalPreviousAlarmVolume == null) globalPreviousAlarmVolume = cur
        am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
    }

    private fun restoreAlarmVolume() {
        val target = globalPreviousAlarmVolume ?: previousAlarmVolume
        target?.let {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }
        previousAlarmVolume = null
        globalPreviousAlarmVolume = null
    }

    private fun startAudio() {
        stopAudio()
        val sampleRate = 44100
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val at = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        at.play()
        audioTrack = at
        audioThread = thread(start = true, isDaemon = true, name = "AlarmTone") {
            try {
                // Generate harsh alternating 2.6kHz-3.4kHz square-ish tone, sweep every ~500ms
                val out = ShortArray(bufferSize / 2)
                var t = 0.0
                var phase = 0.0
                val dt = 1.0 / sampleRate
                val f1 = 2600.0
                val f2 = 3400.0
                var target = f2
                var current = f1
                while (!Thread.currentThread().isInterrupted) {
                    // sweep
                    current += (target - current) * 0.02
                    if (abs(current - target) < 5) {
                        target = if (target == f2) f1 else f2
                    }
                    val freq = current
                    val step = 2 * PI * freq * dt
                    for (i in out.indices) {
                        phase += step
                        val s = sin(phase)
                        // approximate square by sign(sin)
                        val square = sign(s)
                        val value = (square * 0.85 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        out[i] = value.toShort()
                    }
                    try {
                        at.write(out, 0, out.size)
                    } catch (_: Throwable) {
                        break
                    }
                    t += out.size * dt
                }
            } catch (_: Throwable) {
                // ignore emulator audio issues
            }
        }
    }

    private fun stopAudio() {
        audioThread?.interrupt()
        audioThread = null
        audioTrack?.let {
            try { it.stop() } catch (_: Throwable) {}
            try { it.release() } catch (_: Throwable) {}
        }
        audioTrack = null
    }

    private fun stopMedia() {
        mediaPlayer?.let {
            try { it.stop() } catch (_: Throwable) {}
            try { it.release() } catch (_: Throwable) {}
        }
        mediaPlayer = null
        isMusicPlaying = false
    }

    

    // --- Awake-mode support ---
    fun previewAwakeChime(volumePercent: Int) {
        playAwakeChime(volumePercent, durationMs = 2000, loop = false, freqHz = 880.0)
    }

    fun startAwakeChimeLowWarning(volumePercent: Int) {
        playAwakeChime(volumePercent, durationMs = 0, loop = true, freqHz = 880.0)
    }

    fun startAwakeChimeHigh(volumePercent: Int) {
        playAwakeChime(volumePercent, durationMs = 0, loop = true, freqHz = 1320.0)
    }

    fun startAwakeChimeSignal(volumePercent: Int) {
        playAwakeChime(volumePercent, durationMs = 0, loop = true, freqHz = 660.0)
    }

    fun startAwakeRampLow(volumePercentStart: Int, urgent: Boolean) {
        val startPct = if (urgent) maxOf(volumePercentStart, 80) else volumePercentStart
        playAwakeChime(startPct, durationMs = 0, loop = true, freqHz = 880.0)
        startVolumeRamp(startPct, 100, totalMs = 5 * 60 * 1000) {
            // at max: switch to full screech
            stopAwakeChime()
            startAlarm()
        }
    }

    fun stopAwakeChime() {
        stopAudio()
        stopVibration()
        stopRamp()
        restoreAlarmVolume()
    }

    // --- Music Mode (awake only) ---
    fun startAwakeMusicFromUri(volumePercent: Int, uri: Uri) {
        stopAwakeChime()
        stopMedia()
        forceAlarmVolumePercent(volumePercent)
        val mp = MediaPlayer()
        mediaPlayer = mp
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            mp.setAudioAttributes(attrs)
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.setOnPreparedListener {
                it.start()
                isMusicPlaying = true
            }
            mp.setOnCompletionListener { isMusicPlaying = false }
            mp.prepareAsync()
        } catch (_: Throwable) {
            stopMedia()
        }
    }

    fun startAwakeMusicLowDefault(volumePercent: Int) {
        // Generate simple sine melody for "Swing Low, Sweet Chariot"
        stopAwakeChime()
        stopMedia()
        forceAlarmVolumePercent(volumePercent)
        val sampleRate = 44100
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
            4096
        )
        val at = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        at.play()
        audioTrack = at
        isMusicPlaying = true
        audioThread = thread(start = true, isDaemon = true, name = "SwingLowMelody") {
            try {
                val out = ShortArray(bufferSize / 2)
                val dt = 1.0 / sampleRate
                var phase = 0.0
                // Define notes (frequency Hz, duration ms); key of G major (approx)
                data class Note(val freq: Double, val ms: Int)
                val quarter = 500
                val half = 1000
                val E4 = 329.63; val G4 = 392.00; val A4 = 440.00; val B4 = 493.88; val D5 = 587.33; val C5 = 523.25
                val notes = listOf(
                    // "Swing low, sweet cha-ri-ot"
                    Note(G4, half), Note(E4, quarter), Note(G4, quarter),
                    Note(B4, half), Note(A4, half),
                    // "Comin' for to carry me home"
                    Note(G4, half), Note(E4, quarter), Note(G4, quarter),
                    Note(D5, half), Note(B4, half),
                    // small refrain
                    Note(A4, half), Note(G4, half),
                    Note(E4, half), Note(G4, half),
                )
                var idx = 0
                var remainingSamples = 0
                var omega = 2 * Math.PI * notes.first().freq * dt
                while (!Thread.currentThread().isInterrupted) {
                    if (remainingSamples <= 0) {
                        val n = notes[idx]
                        omega = 2 * Math.PI * n.freq * dt
                        remainingSamples = (n.ms / 1000.0 * sampleRate).toInt()
                        idx = (idx + 1) % notes.size
                    }
                    // fill buffer or until note end
                    val samples = out.size
                    for (i in 0 until samples) {
                        phase += omega
                        val env = 0.6 // fixed amplitude
                        val s = Math.sin(phase) * env
                        out[i] = (s * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        remainingSamples--
                        if (remainingSamples <= 0) {
                            // next note on next sample
                            break
                        }
                    }
                    at.write(out, 0, samples)
                }
            } catch (_: Throwable) {
            } finally {
                isMusicPlaying = false
            }
        }
    }

    fun startAwakeMusicHighDefault(volumePercent: Int) {
        // Original 70s-funk-inspired loop "Going Up" (~125 BPM), clavinet stabs + syncopated bass
        stopAwakeChime()
        stopMedia()
        forceAlarmVolumePercent(volumePercent)
        val sampleRate = 44100
        val bpm = 125.0
        val secPerBeat = 60.0 / bpm
        val bars = 8 // 8 bars ~ 15.36s
        val beatsPerBar = 4
        val loopDurSec = bars * beatsPerBar * secPerBeat
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
            8192
        )
        val at = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        at.play()
        audioTrack = at
        isMusicPlaying = true
        audioThread = thread(start = true, isDaemon = true, name = "GoingUpLoop") {
            try {
                val out = ShortArray(bufferSize / 2)
                var tGlobal = 0.0
                val dt = 1.0 / sampleRate
                // Instrument helpers
                fun envPerc(t: Double, attack: Double, decay: Double): Double {
                    return when {
                        t < attack -> t / attack
                        else -> kotlin.math.exp(-(t - attack) / decay)
                    }
                }
                fun square(phase: Double) = if (kotlin.math.sin(phase) >= 0) 1.0 else -1.0
                fun saw(phase: Double) = 2.0 * (phase / (2 * Math.PI) - kotlin.math.floor(phase / (2 * Math.PI) + 0.5))
                // Percussion (very light): kick (60Hz), snare (noise), hat (noise short)
                val rng = java.util.Random(0xF0F0A9BCL)
                fun kick(tBar: Double): Double {
                    // hits on beats 1 and 3
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val near1 = kotlin.math.min(kotlin.math.abs(beat - 0.0), kotlin.math.abs(beat - 2.0))
                    val within = if (near1 < 0.1) 1.0 else 0.0
                    val tt = (beat % 1.0) * secPerBeat
                    val env = envPerc(tt, 0.005, 0.06)
                    val tone = kotlin.math.sin(2 * Math.PI * 60.0 * tt)
                    return within * env * tone * 0.3
                }
                fun snare(tBar: Double): Double {
                    // beats 2 and 4
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val near = kotlin.math.min(kotlin.math.abs(beat - 1.0), kotlin.math.abs(beat - 3.0))
                    if (near < 0.05) {
                        val tt = (beat % 1.0) * secPerBeat
                        val env = envPerc(tt, 0.002, 0.05)
                        val noise = (rng.nextDouble() * 2 - 1)
                        return env * noise * 0.25
                    }
                    return 0.0
                }
                fun hihat(tBar: Double): Double {
                    // 8th notes
                    val beat8 = ((tBar / (secPerBeat / 2.0)) % (beatsPerBar * 2))
                    val frac = beat8 % 1.0
                    val env = if (frac < 0.15) kotlin.math.exp(-frac / 0.06) else 0.0
                    val noise = (rng.nextDouble() * 2 - 1)
                    return env * noise * 0.1
                }
                // Bass pattern (E2, G2, A2, B2) syncopation per bar
                fun bass(tBar: Double): Double {
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val on = when {
                        beat < 0.5 -> 82.41 // E2
                        beat < 1.0 -> 98.00 // G2
                        beat < 1.5 -> 110.00 // A2
                        beat < 2.0 -> 98.00 // G2 ghost
                        beat < 2.5 -> 123.47 // B2
                        beat < 3.25 -> 110.00 // A2 sustained syncopation
                        else -> 82.41 // E2 pickup
                    }
                    val phase = 2 * Math.PI * on * (tBar % secPerBeat)
                    val wave = 0.6 * kotlin.math.sin(phase) + 0.4 * saw(phase)
                    val frac = beat % 0.5
                    val env = envPerc(frac * secPerBeat, 0.005, 0.1)
                    return wave * env * 0.35
                }
                // Clavinet stabs (E4, G4, B4 syncopated 16th hits)
                fun clav(tBar: Double): Double {
                    val beat16 = ((tBar / (secPerBeat / 4.0)) % (beatsPerBar * 4))
                    val which = (beat16.toInt() % 4)
                    val freq = when (which) {
                        0 -> 329.63 // E4
                        1 -> 392.00 // G4
                        2 -> 493.88 // B4
                        else -> 392.00 // G4
                    }
                    val local = (beat16 % 1.0) * (secPerBeat / 4.0)
                    val env = envPerc(local, attack = 0.003, decay = 0.06)
                    val phase = 2 * Math.PI * freq * local
                    val harmonics = square(phase) * 0.7 + kotlin.math.sin(phase * 2) * 0.3
                    return harmonics * env * 0.25
                }
                // Seamless loop by fading the last 50ms to zero
                val fadeTailSec = 0.05
                while (!Thread.currentThread().isInterrupted) {
                    val samples = out.size
                    for (i in 0 until samples) {
                        val tLoop = (tGlobal % loopDurSec)
                        val mix =
                            kick(tLoop) +
                            snare(tLoop) +
                            hihat(tLoop) +
                            bass(tLoop) +
                            clav(tLoop)
                        // simple limiter
                        val limited = mix.coerceIn(-1.0, 1.0)
                        // apply tail fade for last fadeTailSec to make loop seamless
                        val fade =
                            if (tLoop > loopDurSec - fadeTailSec) (1.0 - (tLoop - (loopDurSec - fadeTailSec)) / fadeTailSec)
                            else 1.0
                        out[i] = (limited * fade * Short.MAX_VALUE).toInt().toShort()
                        tGlobal += dt
                        if (tGlobal >= loopDurSec) tGlobal -= loopDurSec
                    }
                    at.write(out, 0, samples)
                }
            } catch (_: Throwable) {
            } finally {
                isMusicPlaying = false
            }
        }
    }

    fun startAwakeMusicSignalDefault(volumePercent: Int) {
        // Original 80s synth-pop loop "Lost Connection" (~130 BPM), gated drums + bright pads
        stopAwakeChime()
        stopMedia()
        forceAlarmVolumePercent(volumePercent)
        val sampleRate = 44100
        val bpm = 130.0
        val secPerBeat = 60.0 / bpm
        val bars = 16 // ~29.5s
        val beatsPerBar = 4
        val loopDurSec = bars * beatsPerBar * secPerBeat
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
            8192
        )
        val at = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        at.play()
        audioTrack = at
        isMusicPlaying = true
        audioThread = thread(start = true, isDaemon = true, name = "LostConnectionLoop") {
            try {
                val out = ShortArray(bufferSize / 2)
                var tGlobal = 0.0
                val dt = 1.0 / sampleRate
                val rng = java.util.Random(0x51AB12CDL)
                fun envPerc(t: Double, attack: Double, decay: Double): Double {
                    return when {
                        t < attack -> t / attack
                        else -> kotlin.math.exp(-(t - attack) / decay)
                    }
                }
                fun gatePulse(t: Double, ratePerBeat: Double, duty: Double): Double {
                    val period = secPerBeat / ratePerBeat
                    val phase = (t % period) / period
                    return if (phase < duty) 1.0 else 0.0
                }
                fun saw(phase: Double) = 2.0 * (phase / (2 * Math.PI) - kotlin.math.floor(phase / (2 * Math.PI) + 0.5))
                // Drums (gated style)
                fun kick(tBar: Double): Double {
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val isHit = (kotlin.math.abs(beat - 0.0) < 0.05 || kotlin.math.abs(beat - 2.0) < 0.05)
                    val tt = (beat % 1.0) * secPerBeat
                    val env = envPerc(tt, 0.003, 0.08)
                    val tone = kotlin.math.sin(2 * Math.PI * 65.0 * tt)
                    return (if (isHit) env * tone else 0.0) * 0.35
                }
                fun snare(tBar: Double): Double {
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val isHit = (kotlin.math.abs(beat - 1.0) < 0.03 || kotlin.math.abs(beat - 3.0) < 0.03)
                    val tt = (beat % 1.0) * secPerBeat
                    val env = envPerc(tt, 0.002, 0.07)
                    val noise = (rng.nextDouble() * 2 - 1)
                    val gated = gatePulse(tBar, ratePerBeat = 2.0, duty = 0.45) // 8th-note gate
                    return (if (isHit) env * noise else 0.0) * gated * 0.28
                }
                fun hat(tBar: Double): Double {
                    val beat16 = ((tBar / (secPerBeat / 4.0)) % (beatsPerBar * 4))
                    val frac = beat16 % 1.0
                    val env = if (frac < 0.12) kotlin.math.exp(-frac / 0.05) else 0.0
                    val noise = (rng.nextDouble() * 2 - 1)
                    val gated = gatePulse(tBar, ratePerBeat = 4.0, duty = 0.4) // 16th gating
                    return env * noise * gated * 0.12
                }
                // Bright detuned pad chords progression across bars (original)
                fun pad(tBar: Double): Double {
                    // 4-chord cycle across bars: roots (D, G, E, F) in higher register to avoid muddy mix
                    val barIdx = ((tGlobal / (secPerBeat * beatsPerBar))).toInt() % 4
                    val rootFreq = when (barIdx) {
                        0 -> 293.66 // D4
                        1 -> 392.00 // G4
                        2 -> 329.63 // E4
                        else -> 349.23 // F4
                    }
                    val fifth = rootFreq * 1.5
                    val octave = rootFreq * 2.0
                    val phaseRoot = 2 * Math.PI * rootFreq * (tBar % (secPerBeat * beatsPerBar))
                    val phaseFifth = 2 * Math.PI * fifth * (tBar % (secPerBeat * beatsPerBar))
                    val phaseOct = 2 * Math.PI * octave * (tBar % (secPerBeat * beatsPerBar))
                    val detune = 0.995
                    val padWave =
                        0.5 * saw(phaseRoot) + 0.5 * saw(phaseRoot * detune) +
                        0.4 * saw(phaseFifth) + 0.3 * saw(phaseOct)
                    // soft attack/decay within bar and gated amplitude
                    val beat = (tBar / secPerBeat) % beatsPerBar
                    val within = (beat % 1.0) * secPerBeat
                    val env = envPerc(within, 0.02, 0.2)
                    val gated = gatePulse(tBar, ratePerBeat = 2.0, duty = 0.5)
                    return padWave * env * gated * 0.25
                }
                // Seamless loop by fading the last 70ms
                val fadeTailSec = 0.07
                while (!Thread.currentThread().isInterrupted) {
                    val samples = out.size
                    for (i in 0 until samples) {
                        val tLoop = (tGlobal % loopDurSec)
                        val mix =
                            kick(tLoop) +
                            snare(tLoop) +
                            hat(tLoop) +
                            pad(tLoop)
                        val limited = mix.coerceIn(-1.0, 1.0)
                        val fade =
                            if (tLoop > loopDurSec - fadeTailSec) (1.0 - (tLoop - (loopDurSec - fadeTailSec)) / fadeTailSec)
                            else 1.0
                        out[i] = (limited * fade * Short.MAX_VALUE).toInt().toShort()
                        tGlobal += dt
                        if (tGlobal >= loopDurSec) tGlobal -= loopDurSec
                    }
                    at.write(out, 0, samples)
                }
            } catch (_: Throwable) {
            } finally {
                isMusicPlaying = false
            }
        }
    }

    fun startAwakeMusicRampLow(volumePercentStart: Int, urgent: Boolean, contentUri: Uri?) {
        // Start music (URI if provided, else default melody), then ramp STREAM_ALARM volume to max over 5 min
        val startPct = if (urgent) maxOf(volumePercentStart, 80) else volumePercentStart
        if (contentUri != null) {
            startAwakeMusicFromUri(startPct, contentUri)
        } else {
            startAwakeMusicLowDefault(startPct)
        }
        startVolumeRamp(startPct, 100, totalMs = 5 * 60 * 1000) {
            // at max: switch to full screech
            stopAwakeMusic()
            startAlarm()
        }
    }

    fun stopAwakeMusic() {
        stopMedia()
        stopAudio()
        stopVibration()
        stopRamp()
        restoreAlarmVolume()
    }

    private fun playAwakeChime(volumePercent: Int, durationMs: Int, loop: Boolean, freqHz: Double) {
        forceAlarmVolumePercent(volumePercent)
        // generate a soft bell-like sine with envelope
        stopAudio()
        val sampleRate = 44100
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
            4096
        )
        val at = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        at.play()
        audioTrack = at
        audioThread = thread(start = true, isDaemon = true, name = "AwakeChime") {
            val out = ShortArray(bufferSize / 2)
            val dt = 1.0 / sampleRate
            var phase = 0.0
            val omega = 2 * PI * freqHz * dt
            val chimePeriod = 1000 // ms per chime
            val decayConst = 3.0 // envelope decay per second
            var playedMs = 0
            fun fillBuffer(): Boolean {
                val samples = out.size
                for (i in 0 until samples) {
                    val tSec = ((playedMs.toDouble()) / 1000.0)
                    val env = kotlin.math.exp(-decayConst * (tSec % 1.0))
                    phase += omega
                    val s = sin(phase) * env
                    out[i] = (s * 0.8 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    playedMs += (1000.0 / sampleRate).toInt()
                }
                at.write(out, 0, samples)
                return true
            }
            if (loop) {
                while (!Thread.currentThread().isInterrupted) {
                    if (!fillBuffer()) break
                }
            } else {
                val endTime = nowProvider() + durationMs
                while (!Thread.currentThread().isInterrupted && nowProvider() < endTime) {
                    if (!fillBuffer()) break
                }
                // auto-restore volume after preview
                restoreAlarmVolume()
            }
        }
    }

    private fun forceAlarmVolumePercent(volumePercent: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
        previousAlarmVolume = cur
        if (globalPreviousAlarmVolume == null) globalPreviousAlarmVolume = cur
        val set = (max * (volumePercent / 100.0)).toInt().coerceIn(1, max)
        am.setStreamVolume(AudioManager.STREAM_ALARM, set, 0)
    }

    private fun startVolumeRamp(startPercent: Int, endPercent: Int, totalMs: Int, onComplete: () -> Unit) {
        stopRamp()
        rampJob = thread(start = true, isDaemon = true, name = "VolumeRamp") {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val start = nowProvider()
            while (true) {
                val elapsed = nowProvider() - start
                val frac = (elapsed.toDouble() / totalMs).coerceIn(0.0, 1.0)
                val curPercent = startPercent + ((endPercent - startPercent) * frac).toInt()
                val set = (max * (curPercent / 100.0)).toInt().coerceIn(1, max)
                am.setStreamVolume(AudioManager.STREAM_ALARM, set, 0)
                if (frac >= 1.0) break
                try { Thread.sleep(500) } catch (_: InterruptedException) { break }
            }
            if (!Thread.currentThread().isInterrupted) onComplete()
        }
    }

    private fun stopRamp() {
        rampJob?.interrupt()
        rampJob = null
    }
}

