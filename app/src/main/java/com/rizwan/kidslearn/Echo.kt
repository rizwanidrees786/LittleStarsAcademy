package com.rizwan.kidslearn

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Hands-free echo like the famous talking pets: it waits for the child to speak, records the real voice,
 * and plays it back faster (= higher pitch). `level` follows the volume so the mouth can move.
 */
class EchoEngine {
    var state by mutableIntStateOf(0)      // 0 off, 1 waiting for a voice, 2 hearing, 3 repeating
    var level by mutableFloatStateOf(0f)

    private fun rms(b: ShortArray, n: Int): Float {
        var sum = 0.0
        for (i in 0 until n) { val v = b[i].toDouble(); sum += v * v }
        return sqrt(sum / maxOf(n, 1)).toFloat()
    }

    @SuppressLint("MissingPermission")
    suspend fun run(pitch: () -> Float, shouldRun: () -> Boolean, paused: () -> Boolean) = withContext(Dispatchers.IO) {
        val sr = 16000
        val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return@withContext
        val rec: AudioRecord = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 8192))
        } catch (_: Throwable) { return@withContext }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@withContext }
        try {
            rec.startRecording()
            val chunk = ShortArray(800)            // 50 ms
            var voice = ArrayList<ShortArray>()
            var talking = false
            var silent = 0
            var floor = 400f
            state = 1
            while (isActive && shouldRun()) {
                val n = rec.read(chunk, 0, chunk.size)
                if (n <= 0) continue
                if (paused()) { voice = ArrayList(); talking = false; silent = 0; continue }
                val e = rms(chunk, n)
                val thr = maxOf(1100f, floor * 3.2f)
                if (e > thr) { talking = true; silent = 0; state = 2 }
                else if (!talking) floor = floor * 0.95f + e * 0.05f
                else silent++
                if (talking) voice.add(chunk.copyOf(n))
                if (talking && (silent >= 13 || voice.size >= 160)) {
                    val keep = voice.size - silent
                    if (keep >= 5) {
                        rec.stop()
                        play(voice.subList(0, keep), sr, pitch())
                        rec.startRecording()
                    }
                    voice = ArrayList(); talking = false; silent = 0; state = 1
                }
            }
        } finally {
            try { rec.stop() } catch (_: Throwable) { }
            rec.release()
            state = 0; level = 0f
        }
    }

    private fun play(parts: List<ShortArray>, sr: Int, pitch: Float) {
        state = 3
        val rate = (sr * pitch).toInt()
        val minOut = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track: AudioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minOut, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) { state = 1; return }
        try {
            track.play()
            for (p in parts) {
                val out = ShortArray(p.size) { i -> (p[i] * 1.6f).toInt().coerceIn(-32768, 32767).toShort() }
                level = (rms(out, out.size) / 6000f).coerceIn(0f, 1f)
                track.write(out, 0, out.size)
            }
            track.stop()
        } finally {
            track.release()
            level = 0f
        }
        Thread.sleep(250)
    }
}
