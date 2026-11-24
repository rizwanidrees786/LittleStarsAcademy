package com.rizwan.kidslearn

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// =============================================================== Model files
class ModelManager(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("models", Context.MODE_PRIVATE)
    // Folder that is kept when the app is updated (and usually after uninstall too)
    val dir: File = run {
        val media = try { ctx.externalMediaDirs.firstOrNull() } catch (_: Throwable) { null }
        File(media ?: ctx.filesDir, "models").apply { mkdirs() }
    }
    private val legacy = File(ctx.filesDir, "models")

    var gpuDisabled: Boolean
        get() = prefs.getBoolean("gpu_off", false)
        set(v) { prefs.edit().putBoolean("gpu_off", v).apply() }
    var gpuPending: Boolean
        get() = prefs.getBoolean("gpu_pending", false)
        set(v) { prefs.edit().putBoolean("gpu_pending", v).commit() }

    init {
        // if the app crashed while starting the GPU engine last time, switch GPU off for good
        if (gpuPending) { gpuDisabled = true; gpuPending = false }
    }

    fun installed(): List<File> =
        listOf(dir, legacy).distinct().flatMap { d ->
            d.listFiles { f -> f.isFile && (f.name.endsWith(".task") || f.name.endsWith(".bin")) }?.toList() ?: emptyList()
        }.sortedBy { it.name }

    var active: String?
        get() = prefs.getString("active", null)
        set(v) { prefs.edit().putString("active", v).apply() }

    fun activeFile(): File? = active?.let { n -> listOf(File(dir, n), File(legacy, n)).firstOrNull { it.exists() } }

    fun delete(f: File) { f.delete(); if (active == f.name) active = null }

    suspend fun download(url: String, token: String, onProgress: (Float) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        try {
            var name = url.substringBefore('?').substringAfterLast('/')
            if (!(name.endsWith(".task") || name.endsWith(".bin"))) name += ".task"
            val firstHost = URL(url).host
            var u = URL(url)
            var auth = token.isNotBlank()
            var conn = u.openConnection() as HttpURLConnection
            var hops = 0
            while (true) {
                conn = u.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 20000
                conn.readTimeout = 30000
                if (auth) conn.setRequestProperty("Authorization", "Bearer ${token.trim()}")
                val code = conn.responseCode
                if (code in 300..399 && hops < 6) {
                    val loc = conn.getHeaderField("Location") ?: throw IllegalStateException("Bad redirect")
                    u = URL(u, loc)
                    if (u.host != firstHost) auth = false
                    conn.disconnect()
                    hops++
                    continue
                }
                if (code !in 200..299) {
                    val hint = if (code == 401 || code == 403) " (login / token / license needed)" else ""
                    return@withContext Result.failure(Exception("Server said $code$hint"))
                }
                break
            }
            val total = conn.contentLengthLong
            val part = File(dir, "$name.part")
            conn.inputStream.use { inp ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read / total.toFloat())
                    }
                }
            }
            File(dir, name).delete()
            part.renameTo(File(dir, name))
            Result.success(name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            var name = "model_${System.currentTimeMillis()}.task"
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) name = c.getString(i)
            }
            if (!(name.endsWith(".task") || name.endsWith(".bin"))) name += ".task"
            val out = File(dir, name)
            ctx.contentResolver.openInputStream(uri)!!.use { inp -> out.outputStream().use { o -> inp.copyTo(o, 64 * 1024) } }
            Result.success(name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// =============================================================== Offline AI engine
class LlmEngine(private val ctx: Context, private val models: ModelManager) {
    private var llm: LlmInference? = null
    var loadedName by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var lastInfo by mutableStateOf("")
    var backend by mutableStateOf("")

    private companion object { const val MAX_TOKENS = 640 }

    /** GPU is requested through reflection so the app still builds on every MediaPipe version. */
    private fun trySetGpu(builder: Any): Boolean = try {
        val cls = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$Backend")
        val gpu = cls.enumConstants!!.first { (it as Enum<*>).name == "GPU" }
        builder.javaClass.getMethod("setPreferredBackend", cls).invoke(builder, gpu)
        true
    } catch (_: Throwable) { false }

    suspend fun load(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            loading = true
            llm?.close(); llm = null; loadedName = null; lastInfo = ""
            var created: LlmInference? = null
            if (!models.gpuDisabled) {
                models.gpuPending = true
                try {
                    val b = LlmInference.LlmInferenceOptions.builder().setModelPath(file.absolutePath).setMaxTokens(MAX_TOKENS)
                    if (trySetGpu(b)) { created = LlmInference.createFromOptions(ctx, b.build()); backend = "GPU" }
                } catch (_: Throwable) { created = null }
                models.gpuPending = false
            }
            if (created == null) {
                val o = LlmInference.LlmInferenceOptions.builder().setModelPath(file.absolutePath).setMaxTokens(MAX_TOKENS).build()
                created = LlmInference.createFromOptions(ctx, o)
                backend = "CPU"
            }
            llm = created
            loadedName = file.name
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "could not load"))
        } finally {
            loading = false
        }
    }

    suspend fun generate(prompt: String): String? = withContext(Dispatchers.IO) {
        val engine = llm ?: return@withContext null
        val t0 = System.currentTimeMillis()
        fun secs() = "%.1f".format((System.currentTimeMillis() - t0) / 1000.0)
        try {
            val so = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed((t0 % 100000).toInt())
                .build()
            val out = LlmInferenceSession.createFromOptions(engine, so).use { sess ->
                sess.addQueryChunk(prompt)
                sess.generateResponse()
            }
            lastInfo = "🧠 AI ($backend) ${secs()}s"
            out
        } catch (e: Throwable) {
            try {
                val out = engine.generateResponse(prompt)
                lastInfo = "🧠 AI ($backend, simple) ${secs()}s"
                out
            } catch (e2: Throwable) {
                lastInfo = "AI error: ${(e2.message ?: e.message ?: "unknown").take(90)}"
                null
            }
        }
    }

    fun unload() { try { llm?.close() } catch (_: Throwable) { }; llm = null; loadedName = null }
}

// =============================================================== Child safety
object Safety {
    private val blocked = setOf(
        "sex", "porn", "naked", "nude", "kill", "murder", "suicide", "drug", "drugs", "cocaine", "weed", "alcohol",
        "beer", "wine", "vodka", "cigarette", "smoking", "gun", "weapon", "bomb", "blood", "rape", "gambling",
        "hate", "stupid", "idiot", "damn", "hell", "die", "dead", "terror"
    )
    private fun words(t: String) = t.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.toSet()
    private val blockedUr = listOf("قتل", "شراب", "سگریٹ", "نشہ", "بندوق", "خون", "مرنا")
    fun isUnsafe(t: String) = words(t).any { it in blocked } || blockedUr.any { t.contains(it) }
    fun redirect(name: String) = "Hmm, that is a question for Mama or Papa, $name! Let's do something fun. Do you want a joke, a story or counting?"

    private val identity = Regex("(who (made|created|built) you|are you (an? )?(ai|robot|bot|computer|real)|which model|what model|chatgpt|gemma|qwen|gemini|openai|google|your (company|creator|brain))", RegexOption.IGNORE_CASE)
    fun identityReply(text: String, b: Buddy): String? =
        if (identity.containsMatchIn(text)) "I am ${b.name}, your cartoon friend from Little Stars Academy! I love to play and learn with you." else null

    private val leak = Regex("(language model|ai model|large language|trained by|as an ai|i am an ai|gemma|qwen|google|alibaba|openai|anthropic|claude|chatgpt|llm)", RegexOption.IGNORE_CASE)
    fun cleanOutput(raw: String): String? {
        var t = raw.substringBefore("<end_of_turn>").substringBefore("<|im_end|>").replace(Regex("<[^>]*>"), "").trim()
        if (t.isEmpty() || isUnsafe(t) || leak.containsMatchIn(t)) return null
        if (t.length > 420) {
            val cut = t.take(420)
            val e = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('!'), cut.lastIndexOf('?'))
            t = if (e > 40) cut.take(e + 1) else cut
        }
        return t
    }
}

// =============================================================== Brain (AI + built-in basic mode)
data class ChatMsg(val fromChild: Boolean, val text: String)

class Brain(private val engine: LlmEngine) {
    var useAi: () -> Boolean = { true }
    var slow by mutableStateOf(false)

    suspend fun reply(p: Profile, b: Buddy, hist: List<ChatMsg>, text: String): String {
        if (Safety.isUnsafe(text)) return Safety.redirect(p.name)
        Safety.identityReply(text, b)?.let { engine.lastInfo = "🛡️ safe reply"; return it }
        MathSolver.solve(text)?.let { engine.lastInfo = "🧮 maths (instant)"; return it }
        Script.command(p, b, text)?.let { engine.lastInfo = "💬 built-in (instant)"; return it }
        val hits = KnowledgeBase.search(text, 3)
        val top = hits.firstOrNull()
        if (top != null && top.score >= 0.72f) { engine.lastInfo = "📚 syllabus (instant)"; return top.a }

        var waited = 0
        while (useAi() && engine.loading && waited < 90_000) { delay(300); waited += 300 }
        val model = if (useAi()) engine.loadedName else null
        if (model != null) {
            val facts = hits.filter { it.score >= 0.35f }.take(2).map { it.a }
            val t0 = System.currentTimeMillis()
            val raw = engine.generate(buildPrompt(model, system(p, b), facts, hist, text))
            if (System.currentTimeMillis() - t0 > 15000L) slow = true
            val clean = raw?.let { Safety.cleanOutput(it) }
            if (clean != null) return clean
            if (raw != null) engine.lastInfo += " | filtered: " + raw.take(40).replace("\n", " ")
        }
        if (top != null && top.score >= 0.45f) { engine.lastInfo += " | 📚"; return top.a }
        return Script.fallback(p, b)
    }

    private fun system(p: Profile, b: Buddy): String {
        val who = when (p.level) {
            1 -> "a 3 year old"
            2 -> "a 5 year old"
            else -> "a 7 year old"
        }
        return "You are ${b.name}, a cheerful cartoon friend of ${p.name}, $who child. " +
            "Reply in 1 or 2 very short, simple English sentences, then ask one easy question. " +
            "Be kind and praise the child. Only child-safe topics. For scary or adult things say: ask Mama or Papa. " +
            "Never say you are a computer, AI or model."
    }

    private fun buildPrompt(modelName: String, system: String, facts: List<String>, hist: List<ChatMsg>, user: String): String {
        val h = hist.takeLast(4)
        val factText = if (facts.isEmpty()) "" else "Facts you may use: " + facts.joinToString(" ")
        return if (modelName.contains("qwen", true) || modelName.contains("chatml", true)) {
            val sb = StringBuilder("<|im_start|>system\n$system $factText<|im_end|>\n")
            h.forEach { sb.append("<|im_start|>${if (it.fromChild) "user" else "assistant"}\n${it.text}<|im_end|>\n") }
            sb.append("<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n").toString()
        } else {
            val sb = StringBuilder("<start_of_turn>user\n$system\n")
            if (factText.isNotEmpty()) sb.append(factText).append("\n")
            if (h.isNotEmpty()) {
                sb.append("Conversation so far:\n")
                h.forEach { sb.append(if (it.fromChild) "Child: " else "Buddy: ").append(it.text).append("\n") }
            }
            sb.append("\nChild: $user<end_of_turn>\n<start_of_turn>model\n").toString()
        }
    }
}

/** Works with no model installed: simple, safe, scripted replies. */
object Script {
    fun command(p: Profile, b: Buddy, text: String): String? {
        val t = text.lowercase().trim()
        fun w(vararg k: String) = Regex("\\b(" + k.joinToString("|") + ")\\b").containsMatchIn(t)

        Regex("spell (\\w+)").find(t)?.let { m ->
            val x = m.groupValues[1].uppercase()
            return "$x is spelled: ${x.toCharArray().joinToString(" - ")}. Can you spell it?"
        }
        Regex("what is (\\w+) in english").find(t)?.let { m ->
            Content.urdu.firstOrNull { it.first.lowercase() == m.groupValues[1] }?.let { return "${it.first} means ${it.third} in English. Say it: ${it.third}!" }
        }
        Regex("(\\w+) in urdu").find(t)?.let { m ->
            Content.urdu.firstOrNull { it.third.lowercase() == m.groupValues[1] }?.let { return "${it.third} in Urdu is ${it.first}, ${it.second}. Say it: ${it.first}!" }
        }
        return when {
            w("hi", "hello", "hey", "salam", "assalam") -> "Hello ${p.name}! I am ${b.name}. How are you today? Aap kaise hain?"
            w("how are you") -> "I am very happy because you are here! How are you, ${p.name}?"
            w("my name") -> "Your name is ${p.name}! What a lovely name."
            w("your name", "who are you") -> "My name is ${b.name}. ${b.name}. Can you say ${b.name}?"
            w("joke") -> Content.jokes.random()
            w("story") -> Content.stories.random().let { it.title + ". " + it.lines.joinToString(" ") { l -> l.en } }
            w("teach me a word", "new word", "word of the day") -> Content.urdu.random().let { "Today's word: ${it.first} (${it.second}) means ${it.third}. Say ${it.third} three times!" }
            w("count", "counting", "numbers") -> {
                val n = if (p.level == 1) 5 else 10
                "Let's count! " + (1..n).joinToString(", ") + ". Now you count to $n!"
            }
            w("thanks", "thank") -> "You are welcome, ${p.name}! You are so polite."
            w("bye", "goodbye") -> "Bye bye ${p.name}! Khuda hafiz! See you soon."
            else -> null
        }
    }

    fun fallback(p: Profile, b: Buddy): String = listOf(
        "I am not sure about that, ${p.name}. Ask me to spell a word, count, tell a joke, or tell a story!",
        "Hmm, let's learn something new! Try asking: what is the capital of Pakistan?",
        "Good question, ${p.name}! Ask me about animals, colors, numbers or Urdu words.",
        "Let's play! Say: spell cat, or: what is water in Urdu?",
        "I love talking with you, ${p.name}! Ask me for a story or a rhyme."
    ).random()

    fun reply(p: Profile, b: Buddy, text: String): String = command(p, b, text) ?: fallback(p, b)
}

// =============================================================== Chat screen
@Composable
fun ChatScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val msgs = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || thinking) return
        input = ""
        val hist = msgs.toList()
        msgs.add(ChatMsg(true, t))
        thinking = true
        scope.launch {
            val r = s.brain.reply(profile, s.buddy, hist, t)
            msgs.add(ChatMsg(false, r))
            thinking = false
            s.say(r)
        }
    }

    var urdu by remember { mutableStateOf(false) }
    var micStatus by remember { mutableStateOf("") }
    val toggleMic = rememberMic(s, onResult = { send(it) }, onStatus = { micStatus = it })

    LaunchedEffect(Unit) {
        s.ensureModelLoaded()
        if (msgs.isEmpty()) {
            val g = "Hello ${profile.name}! I am ${s.buddy.name}. Let's talk! Aao baat karein!"
            msgs.add(ChatMsg(false, g)); s.say(g)
        }
    }
    LaunchedEffect(msgs.size, thinking) {
        val n = msgs.size + (if (thinking) 1 else 0)
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    Column(Modifier.fillMaxSize().padding(14.dp).imePadding()) {
        TopBar("Chat with ${s.buddy.name}", onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BuddyAvatar(s.buddy, 64.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                when {
                    s.engine.loading -> "Waking up my brain…"
                    s.engine.loadedName != null -> "🧠 AI: ${s.engine.loadedName}"
                    else -> "Basic mode (no AI model yet)"
                },
                color = SunYellow, fontSize = 13.sp
            )
        }
        if (s.engine.lastInfo.isNotEmpty()) Text(s.engine.lastInfo, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        if (s.brain.slow && s.aiOn) Text("⚠ AI is slow on this phone. Tap 🧠 AI: OFF for instant answers from the syllabus.", color = SunYellow, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs) { m -> Bubble(m) }
            if (thinking) item { Bubble(ChatMsg(false, "…thinking…")) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("😂 Joke", false, Modifier.width(100.dp)) { send("Tell me a joke") }
            Chip("🔤 Word", false, Modifier.width(100.dp)) { send("Teach me a word") }
            Chip("🔢 Count", false, Modifier.width(100.dp)) { send("Let's count") }
            Chip("📖 Story", false, Modifier.width(100.dp)) { send("Tell me a story") }
            Chip("🎤 English", !urdu, Modifier.width(110.dp)) { urdu = false }
            Chip("🎤 اردو", urdu, Modifier.width(110.dp)) { urdu = true }
            Chip(if (s.aiOn) "🧠 AI: ON" else "🧠 AI: OFF", s.aiOn, Modifier.width(120.dp)) { s.toggleAi() }
        }
        Spacer(Modifier.height(8.dp))
        if (micStatus.isNotEmpty()) Text(micStatus, color = SunYellow, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicButton(s.voice.listening, size = 56.dp) { toggleMic(if (urdu) "ur-PK" else "en-US") }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Type or tap 🎤") }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            GradientButton("➤") { send(input) }
        }
    }
}

@Composable
fun Bubble(m: ChatMsg) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromChild) Arrangement.End else Arrangement.Start) {
        val bg = if (m.fromChild) Brush.horizontalGradient(listOf(Purple, Pink))
        else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.16f)))
        Box(Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(m.text, color = Color.White, fontSize = 17.sp)
        }
    }
}

// =============================================================== Models screen (inside Parent Zone)
private val PRESETS = listOf(
    "Gemma 3 · 1B (small, ~550 MB)" to "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
    "Qwen 2.5 · 1.5B (~1.5 GB)" to "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
    "Gemma 3n · E2B (big, ~3 GB)" to "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
)

@Composable
fun ModelsScreen(s: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var status by remember { mutableStateOf("") }
    val files = remember(tick) { s.models.installed() }

    fun useModel(f: File) {
        s.models.active = f.name
        scope.launch {
            status = "Loading ${f.name}…"
            s.engine.load(f)
                .onSuccess { status = "Ready ✔  ${f.name} is now the brain of all characters."; tick++ }
                .onFailure { status = "Could not load: ${it.message}" }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            status = "Copying file… please wait"
            s.models.importFile(uri)
                .onSuccess { status = "Imported $it. Tap Use."; tick++ }
                .onFailure { status = "Import failed: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("AI Models", onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    when {
                        s.engine.loading -> "Loading model…"
                        s.engine.loadedName != null -> "Active: ${s.engine.loadedName}"
                        else -> "No AI model active (Basic mode works offline)"
                    }, color = Color.White, fontWeight = FontWeight.Bold
                )
                if (status.isNotEmpty()) Text(status, color = SunYellow, fontSize = 13.sp)
            }
            var gpuOff by remember { mutableStateOf(s.models.gpuDisabled) }
            Spacer(Modifier.height(8.dp))
            Chip(if (gpuOff) "⚡ GPU: OFF  (tap to turn on)" else "⚡ GPU speed-up: ON  (tap to turn off)", !gpuOff, Modifier.fillMaxWidth()) {
                gpuOff = !gpuOff; s.models.gpuDisabled = gpuOff
            }
            Text("Applies the next time a model is loaded (tap Use). If the phone cannot use GPU, the app switches it off by itself.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Text("Installed models", color = Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (files.isEmpty()) Text("None yet.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            files.forEach { f ->
                GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(f.name, color = Color.White, fontSize = 14.sp)
                    Text("${f.length() / 1_000_000} MB" + if (s.models.active == f.name) "  ·  in use" else "", color = SunYellow, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Use", true, Modifier.width(90.dp)) { useModel(f) }
                        Chip("Delete", false, Modifier.width(90.dp)) {
                            if (s.engine.loadedName == f.name) s.engine.unload()
                            s.models.delete(f); tick++
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Download a model", color = Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PRESETS.forEach { (label, u) -> Chip(label, url == u, Modifier.fillMaxWidth().padding(vertical = 3.dp)) { url = u } }
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Model link (.task)") }, singleLine = true)
            OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(), label = { Text("HuggingFace token (only if needed)") },
                singleLine = true, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(8.dp))
            progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)), color = Teal, trackColor = Color.White.copy(alpha = 0.2f)) }
            GradientButton(if (progress != null) "Downloading… ${((progress ?: 0f) * 100).toInt()}%" else "⬇ Download", Modifier.fillMaxWidth()) {
                if (progress == null && url.startsWith("https://")) scope.launch {
                    status = "Starting download…"; progress = 0f
                    s.models.download(url.trim(), token) { p -> progress = p }
                        .onSuccess { status = "Downloaded $it. Tap Use."; tick++ }
                        .onFailure { status = "Download failed: ${it.message}" }
                    progress = null
                }
            }
            Spacer(Modifier.height(8.dp))
            GradientButton("📁 Choose model file from phone", Modifier.fillMaxWidth(), Color(0xFF00897B), Teal) { picker.launch(arrayOf("*/*")) }
            Spacer(Modifier.height(10.dp))
            Text(
                "Tips: Start with the smallest model. Gemma models need a free HuggingFace account: open the model page, accept the license, " +
                    "create a Read token (Settings → Access Tokens) and paste it above. Or download the .task file in your browser and use " +
                    "\"Choose model file from phone\". After the model is on the phone, chat works fully offline.",
                color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp
            )
        }
    }
}
