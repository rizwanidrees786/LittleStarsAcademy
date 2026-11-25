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

fun isArabicChar(c: Char): Boolean {
    val x = c.code
    return x in 0x0600..0x06FF || x in 0x0750..0x077F || x in 0xFB50..0xFDFF || x in 0xFE70..0xFEFF
}
fun hasArabic(t: String) = t.any { isArabicChar(it) }

/** Text that is shown on the screen: the ⟦ ⟧ marks (Arabic of duas and Quran) are only for the voice. */
fun plain(t: String) = t.replace("⟦", "").replace("⟧", "")

/** Voice from the phone's own Text-to-Speech. `speaking` drives the characters' mouths. */
class Speaker(context: Context) {
    // kind: 0 = English, 1 = Urdu, 2 = Arabic (dua / Quran, written between ⟦ ⟧)
    private data class Seg(val text: String, val kind: Int)

    private var tts: TextToSpeech? = null
    private var ready = false
    private var stamp = 0
    private val handler = Handler(Looper.getMainLooper())
    private var chain: ArrayDeque<Seg> = ArrayDeque()
    private var chainPitch = 1.1f
    private var chainRate = 0.85f
    var urduAvailable by mutableStateOf(false)
    var arabicAvailable by mutableStateOf(false)
    var speaking by mutableStateOf(false)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val u = tts?.isLanguageAvailable(Locale("ur", "PK")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                urduAvailable = u >= TextToSpeech.LANG_AVAILABLE
                val a = tts?.isLanguageAvailable(Locale("ar")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                arabicAvailable = a >= TextToSpeech.LANG_AVAILABLE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onDone(utteranceId: String?) { finished(utteranceId) }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { finished(utteranceId) }
                })
            }
        }
    }

    private fun finished(id: String?) {
        if (id == null) return
        if (id.startsWith("c")) {
            val my = id.substring(1).toIntOrNull() ?: return
            handler.post { nextInChain(my) }
        } else if (id == "u$stamp") speaking = false
    }

    private fun splitScripts(text: String): List<Seg> {
        val out = ArrayList<Seg>()
        val sb = StringBuilder()
        var cur = false
        var started = false
        for (ch in text) {
            val kind = when { isArabicChar(ch) && !ch.isWhitespace() -> 1; ch.isLetter() -> 0; else -> -1 }
            if (kind != -1 && started && (kind == 1) != cur) { out.add(Seg(sb.toString().trim(), if (cur) 1 else 0)); sb.setLength(0) }
            if (kind != -1) { cur = kind == 1; started = true }
            sb.append(ch)
        }
        if (sb.isNotBlank()) out.add(Seg(sb.toString().trim(), if (cur) 1 else 0))
        return out.filter { seg -> seg.text.any { it.isLetterOrDigit() } }
    }

    private fun splitAll(text: String): List<Seg> {
        val out = ArrayList<Seg>()
        var i = 0
        while (i < text.length) {
            val a = text.indexOf('⟦', i)
            if (a < 0) { out.addAll(splitScripts(text.substring(i))); break }
            if (a > i) out.addAll(splitScripts(text.substring(i, a)))
            val b = text.indexOf('⟧', a)
            val end = if (b < 0) text.length else b
            val arabic = text.substring(a + 1, end).trim()
            if (arabic.isNotEmpty()) out.add(Seg(arabic, 2))
            i = if (b < 0) text.length else b + 1
        }
        return out
    }

    /** English, Urdu and Arabic in the same text: each part is spoken with its own voice. */
    fun speakAuto(text: String, pitch: Float = 1.1f, rate: Float = 0.85f) {
        if (!hasArabic(text) && !text.contains('⟦')) { speakEn(text, pitch, rate); return }
        if (!ready) return
        stamp++
        val my = stamp
        chain = ArrayDeque(splitAll(text).filter { it.kind == 0 || urduAvailable || (it.kind == 2 && arabicAvailable) })
        chainPitch = pitch; chainRate = rate
        speaking = true
        nextInChain(my)
        handler.postDelayed({ if (stamp == my) speaking = false }, text.length * 90L + 3000L)
    }

    private fun nextInChain(my: Int) {
        if (my != stamp) return
        val seg = chain.removeFirstOrNull()
        val t = tts
        if (seg == null || t == null) { speaking = false; return }
        t.language = when {
            seg.kind == 2 && arabicAvailable -> Locale("ar")
            seg.kind >= 1 -> Locale("ur", "PK")
            else -> Locale.US
        }
        t.setPitch(chainPitch)
        t.setSpeechRate(if (seg.kind == 2) 0.65f else if (seg.kind == 1) 0.8f else chainRate)
        t.speak(seg.text, TextToSpeech.QUEUE_FLUSH, null, "c$my")
    }

    fun speakEn(text: String, pitch: Float = 1.1f, rate: Float = 0.85f) = speak(text, Locale.US, pitch, rate)
    fun speakUr(text: String) { if (urduAvailable) speak(text, Locale("ur", "PK"), 1.1f, 0.8f) }

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
        handler.postDelayed({ if (stamp == my) speaking = false }, text.length * 85L + 1500L)
    }

    fun stop() { stamp++; tts?.stop(); speaking = false }
    fun shutdown() { tts?.stop(); tts?.shutdown() }
}
