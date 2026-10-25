package com.g7nightalarm.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var previousMusicVolume: Int? = null

    fun ensure() {
        if (tts == null) {
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.US
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    fun speak(message: String, tempVolumePercent: Int? = null) {
        ensure()
        if (!ready) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (tempVolumePercent != null) {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            previousMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val set = (max * (tempVolumePercent / 100.0)).toInt().coerceIn(1, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, set, 0)
        }
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "g7-tts")
        if (tempVolumePercent != null) {
            // Restore later after a short delay
            android.os.Handler(context.mainLooper).postDelayed({
                previousMusicVolume?.let { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
                previousMusicVolume = null
            }, 3000)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

