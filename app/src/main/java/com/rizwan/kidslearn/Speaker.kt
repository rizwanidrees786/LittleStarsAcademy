package com.rizwan.kidslearn

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Offline voice using the phone's own Text-to-Speech (install offline voice data in phone settings). */
class Speaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    var urduAvailable by mutableStateOf(false)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val r = tts?.isLanguageAvailable(Locale("ur", "PK")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                urduAvailable = r >= TextToSpeech.LANG_AVAILABLE
            }
        }
    }

    fun speakEn(text: String, pitch: Float = 1.1f, rate: Float = 0.85f) = speak(text, Locale.US, pitch, rate)
    fun speakUr(text: String) { if (urduAvailable) speak(text, Locale("ur", "PK"), 1.1f, 0.85f) }

    private fun speak(text: String, locale: Locale, pitch: Float, rate: Float) {
        val t = tts ?: return
        if (!ready) return
        t.language = locale
        t.setPitch(pitch)
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u${System.nanoTime()}")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.stop(); tts?.shutdown() }
}
