package com.rizwan.kidslearn

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Voice from the phone's own Text-to-Speech. `speaking` drives the characters' mouths. */
class Speaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var stamp = 0
    private val handler = Handler(Looper.getMainLooper())
    var urduAvailable by mutableStateOf(false)
    var speaking by mutableStateOf(false)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val r = tts?.isLanguageAvailable(Locale("ur", "PK")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                urduAvailable = r >= TextToSpeech.LANG_AVAILABLE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onDone(utteranceId: String?) { if (utteranceId == "u$stamp") speaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { if (utteranceId == "u$stamp") speaking = false }
                })
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
        stamp++
        val my = stamp
        speaking = true
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u$my")
        // safety net in case the engine never reports "done"
        handler.postDelayed({ if (stamp == my) speaking = false }, text.length * 85L + 1500L)
    }

    fun stop() { stamp++; tts?.stop(); speaking = false }
    fun shutdown() { tts?.stop(); tts?.shutdown() }
}
