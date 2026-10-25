package com.g7nightalarm.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.vosk.Recognizer
import org.json.JSONObject
import java.io.File
import com.g7nightalarm.app.voice.VoiceModelManager.modelDir
import org.vosk.Model

class VoiceKeywordListener(
    private val context: Context,
    private val modeRepository: ModeRepository,
    private val tts: TtsManager
) {
    private var job: Job? = null
    @Volatile private var paused: Boolean = false

    fun startIfEnabledAndReady(voiceEnabled: Boolean) {
        if (!voiceEnabled) return
        if (job != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO not granted; not starting voice listener")
            return
        }
        val modelDir = modelDir(context)
        if (!modelDir.exists()) return
        job = CoroutineScope(Dispatchers.Default).launch {
            try {
                val model = Model(modelDir.absolutePath)
                val recognizer = Recognizer(model, 16000.0f, GRAMMAR_JSON)
                val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                val buffer = ShortArray(minBuf / 2)
                recorder.startRecording()
                while (true) {
                    if (paused) {
                        Thread.sleep(200)
                        continue
                    }
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n > 0) {
                        if (recognizer.acceptWaveForm(buffer, n)) {
                            val res = recognizer.result
                            parseResultAndAct(res)
                        } else {
                            val partial = recognizer.partialResult
                            // optionally parse partial
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Voice listener failed", t)
            }
        }
    }

    private fun parseResultAndAct(json: String) {
        try {
            val obj = JSONObject(json)
            val text = obj.optString("text", "").lowercase()
            when (text.trim()) {
                "good night" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        modeRepository.setMode(AppMode.SLEEP, ModeTrigger.VOICE)
                        tts.speak("Good night, sleep mode on", 60)
                    }
                }
                "good morning" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        modeRepository.setMode(AppMode.AWAKE, ModeTrigger.VOICE)
                        tts.speak("Good morning, sleep mode off", 60)
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        private const val TAG = "VoiceKeywordListener"
        // restrict grammar to two keywords
        private const val GRAMMAR_JSON = "[\"good night\", \"good morning\"]"
    }
}

