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
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var rampJob: Thread? = null
    /**
     * Time source used for testing the awake-mode volume ramp. Defaults to System.currentTimeMillis.
     * Tests may override this to simulate passage of time deterministically.
     */
    var nowProvider: () -> Long = { System.currentTimeMillis() }

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
        previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
    }

    private fun restoreAlarmVolume() {
        previousAlarmVolume?.let {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }
        previousAlarmVolume = null
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
            // Generate harsh alternating 2.6kHz-3.4kHz square-ish tone, sweep every ~500ms
            val out = ShortArray(bufferSize / 2)
            var t = 0.0
            var phase = 0.0
            val dt = 1.0 / sampleRate
            var f1 = 2600.0
            var f2 = 3400.0
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
                at.write(out, 0, out.size)
                t += out.size * dt
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

    companion object {
        private const val ALARM_NOTIF_ID = 1001
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
        previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
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

