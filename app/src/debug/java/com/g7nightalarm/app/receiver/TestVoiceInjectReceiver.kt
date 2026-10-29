package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.mode.AppMode
import com.g7nightalarm.app.mode.ModeRepository
import com.g7nightalarm.app.mode.ModeTrigger
import com.g7nightalarm.app.voice.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only receiver to inject a recognized voice phrase without using microphone/Vosk.
 * Supports "good night" and "good morning".
 */
class TestVoiceInjectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TEST_VOICE) return
        val phrase = intent.getStringExtra(EXTRA_PHRASE)?.lowercase()?.trim() ?: return
        val tts = TtsManager(context)
        val repo = ModeRepository(context)
        when (phrase) {
            "good night" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repo.setMode(AppMode.SLEEP, ModeTrigger.VOICE)
                    tts.speakMuted("Good night, sleep mode on")
                }
            }
            "good morning" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repo.setMode(AppMode.AWAKE, ModeTrigger.VOICE)
                    tts.speakMuted("Good morning, sleep mode off")
                }
            }
        }
    }
    companion object {
        const val ACTION_TEST_VOICE = "com.g7nightalarm.TEST_VOICE"
        const val EXTRA_PHRASE = "phrase"
    }
}

