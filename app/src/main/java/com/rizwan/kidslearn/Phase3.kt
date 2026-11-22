package com.rizwan.kidslearn

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

const val SPEAK = "Speaking"

// =============================================================== Voice input (phone's speech recognizer, prefers offline)
class VoiceInput(private val ctx: Context) {
    private var rec: SpeechRecognizer? = null
    var listening by mutableStateOf(false)
    private var token = 0
    private val handler = Handler(Looper.getMainLooper())

    private data class Attempt(val google: Boolean, val lang: String, val offline: Boolean)

    private val help = "I could not start the microphone. Ask Papa: update the Google app and switch on internet " +
        "(or download the speech pack: Google app > Settings > Voice > Offline speech recognition)."

    fun start(lang: String, onText: (String) -> Unit, onFail: (String) -> Unit) {
        destroy()
        val alt = when (lang) { "en-US" -> "en-IN"; "ur-PK" -> "ur-IN"; else -> lang }
        val attempts = listOf(
            Attempt(true, lang, true), Attempt(true, lang, false),
            Attempt(false, lang, false), Attempt(false, alt, false)
        )
        token++
        tryAttempt(attempts, 0, token, onText, onFail)
    }

    private fun createRecognizer(google: Boolean): SpeechRecognizer? = try {
        if (google) {
            val cn = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.voicesearch.serviceapi.GoogleRecognitionService")
            ctx.packageManager.getServiceInfo(cn, 0)
            SpeechRecognizer.createSpeechRecognizer(ctx, cn)
        } else if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
            SpeechRecognizer.createSpeechRecognizer(ctx)
        } else null
    } catch (_: Throwable) { null }

    private fun tryAttempt(list: List<Attempt>, i: Int, my: Int, onText: (String) -> Unit, onFail: (String) -> Unit) {
        if (my != token) return
        try { rec?.destroy() } catch (_: Throwable) { }
        rec = null
        if (i >= list.size) { listening = false; onFail(help); return }
        val a = list[i]
        val r = createRecognizer(a.google)
        if (r == null) { tryAttempt(list, i + 1, my, onText, onFail); return }
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (my != token) return
                when (error) {
                    6, 7 -> { listening = false; onFail("I did not hear you. Try again!") }
                    9 -> { listening = false; onFail("Please allow the microphone.") }
                    else -> handler.post { tryAttempt(list, i + 1, my, onText, onFail) }
                }
            }
            override fun onResults(results: Bundle?) {
                if (my != token) return
                listening = false
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (t.isEmpty()) onFail("I did not hear you. Try again!") else onText(t)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, a.lang)
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, a.offline)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        listening = true
        try { r.startListening(intent) } catch (_: Throwable) { handler.post { tryAttempt(list, i + 1, my, onText, onFail) } }
    }

    fun stop() { try { rec?.stopListening() } catch (_: Throwable) { }; listening = false }
    fun destroy() { token++; try { rec?.destroy() } catch (_: Throwable) { }; rec = null; listening = false }
}

/** Returns a function: toggleMic(languageTag). Handles the microphone permission. */
@Composable
fun rememberMic(s: AppState, onResult: (String) -> Unit, onStatus: (String) -> Unit): (String) -> Unit {
    val ctx = LocalContext.current
    var pendingLang by remember { mutableStateOf("en-US") }
    fun start(lang: String) {
        s.speaker.stop()
        onStatus("🎤 Listening… speak now")
        s.voice.start(lang, { t -> onStatus(""); onResult(t) }, { m -> onStatus(m) })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) start(pendingLang) else onStatus("Please allow the microphone (ask Mama or Papa).")
    }
    return { lang ->
        if (s.voice.listening) {
            s.voice.stop(); onStatus("")
        } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start(lang)
        } else {
            pendingLang = lang
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun MicButton(listening: Boolean, modifier: Modifier = Modifier, size: Dp = 84.dp, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "mic")
    val pulse by t.animateFloat(1f, 1.18f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
    Box(
        modifier.size(size).scale(if (listening) pulse else 1f).clip(CircleShape)
            .background(Brush.linearGradient(if (listening) listOf(Bad, Pink) else listOf(Teal, Sky)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(if (listening) "⏹" else "🎤", fontSize = (size.value * 0.45f).sp) }
}

// =============================================================== Speak & Practice
private fun speakItems(level: Int): List<Pair<String, String>> = when (level) {
    1 -> listOf("🐱" to "Cat", "🐶" to "Dog", "☀️" to "Sun", "⚽" to "Ball", "🍎" to "Apple", "🐟" to "Fish", "🌙" to "Moon", "🌳" to "Tree")
    2 -> listOf("🐘" to "Elephant", "🌈" to "Rainbow", "🍌" to "Banana", "☂️" to "Umbrella", "🍕" to "Pizza", "🟡" to "Yellow", "🦋" to "Butterfly", "🍊" to "Orange")
    else -> listOf("🍎" to "I like apples", "🐱" to "The cat is big", "🌞" to "Good morning", "👨‍👩‍👧" to "I love my family", "☀️" to "The sun is hot", "🙏" to "Thank you very much", "👋" to "How are you", "🐶" to "The dog can run")
}

private fun speechMatches(target: String, heard: String): Boolean {
    fun tok(x: String) = x.lowercase().replace(Regex("[^a-z ]"), " ").split(" ").filter { it.isNotEmpty() }
    val t = tok(target)
    val h = tok(heard).toSet()
    if (t.isEmpty()) return false
    return t.count { it in h }.toFloat() / t.size >= 0.75f
}

@Composable
fun SpeakScreen(s: AppState, onExit: () -> Unit) {
    val profile = s.profile ?: return
    val progress = remember { s.store.progress(profile.id) }
    val items = remember { speakItems(profile.level).shuffled().take(6) }
    var idx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    var heard by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var confetti by remember { mutableIntStateOf(0) }
    var advance by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    val item = items[idx.coerceAtMost(items.lastIndex)]

    fun save() { s.store.saveProgress(profile.id, progress); s.progressVersion++ }

    fun judge(text: String) {
        heard = text
        tries++
        val q = Question(SPEAK, "Say ${item.second}", "", "", "", emptyList(), "")
        if (speechMatches(item.second, text)) {
            progress.record(q, true); progress.stars++; score++; confetti++
            status = "✅ Perfect!"
            s.say(Content.cheer())
            save(); advance++
        } else if (tries >= 2) {
            progress.record(q, false)
            status = "Let's learn it together."
            s.say("The word is ${item.second}. Listen. ${item.second}.")
            save(); advance++
        } else {
            status = "Almost! Listen and try again."
            s.say(item.second)
        }
    }

    val toggleMic = rememberMic(s, onResult = { judge(it) }, onStatus = { status = it })

    LaunchedEffect(idx, round) { delay(400); s.say("Say: ${item.second}") }
    LaunchedEffect(advance) {
        if (advance > 0) {
            delay(2600)
            tries = 0; heard = ""; status = ""
            if (idx + 1 >= items.size) {
                done = true
                s.say("Great speaking, ${profile.name}! You said $score words right.")
            } else idx++
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) {
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(if (score >= 5) "🏆" else "🎉", fontSize = 90.sp)
                Text("$score / ${items.size}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Text("+$score ⭐ earned!", color = SunYellow, fontSize = 22.sp)
                Spacer(Modifier.height(24.dp))
                GradientButton("Home", Modifier.fillMaxWidth(), Color(0xFF00897B), Teal, onClick = onExit)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip("✕", false, Modifier.width(56.dp)) { s.voice.stop(); onExit() }
                    Spacer(Modifier.width(12.dp))
                    LinearProgressIndicator(progress = { (idx + 1) / items.size.toFloat() }, modifier = Modifier.weight(1f).height(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)), color = Teal, trackColor = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.width(12.dp))
                    Text("⭐ $score", color = SunYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                BuddyAvatar(s.buddy, 84.dp)
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Say it!", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(item.first, fontSize = 64.sp, textAlign = TextAlign.Center, color = Color.White)
                    Text(item.second, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                GradientButton("🔊 Listen", c1 = Color(0xFFFF6F00), c2 = Color(0xFFFFB300)) { s.say(item.second) }
                Spacer(Modifier.height(18.dp))
                MicButton(s.voice.listening) { toggleMic("en-US") }
                Spacer(Modifier.height(8.dp))
                Text(if (s.voice.listening) "Tap to stop" else "Tap the mic and say it", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                if (heard.isNotEmpty()) Text("I heard: $heard", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                if (status.isNotEmpty()) Text(status, color = SunYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Confetti(confetti)
    }
}
