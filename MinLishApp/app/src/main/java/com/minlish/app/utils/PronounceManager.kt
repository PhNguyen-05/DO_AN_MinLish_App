package com.minlish.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.util.Locale

import android.media.AudioManager

class PronounceManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                }
            }
        }
    }

    fun pronounce(word: String, audioUrl: String? = null) {
        // 0. Ensure system media volume is at a reasonable level (at least 70% of max)
        try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * 0.7f).toInt()
            if (currentVolume < targetVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            }
        } catch (e: Exception) {
            // Ignore volume setting if disallowed by system policies
        }

        // 1. Stop any currently active text-to-speech
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // 2. Stop and release any active MediaPlayer
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }

        val resolvedAudioUrl = com.minlish.app.data.remote.RetrofitClient.resolveServerUrl(audioUrl)
        if (!resolvedAudioUrl.isNullOrBlank()) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setVolume(1.0f, 1.0f)
                    setDataSource(resolvedAudioUrl)
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        release()
                        if (mediaPlayer == this) {
                            mediaPlayer = null
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        speakTts(word)
                        release()
                        if (mediaPlayer == this) {
                            mediaPlayer = null
                        }
                        true
                    }
                }
                mediaPlayer = mp
                return
            } catch (e: Exception) {
                // Fallback to TTS on setup error
            }
        }
        speakTts(word)
    }

    private fun speakTts(word: String) {
        if (isTtsReady) {
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, params, "tts_pronounce")
        }
    }

    fun shutdown() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }
        tts?.shutdown()
    }
}
