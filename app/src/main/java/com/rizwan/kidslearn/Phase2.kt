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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// =============================================================== Model files
class ModelManager(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("models", Context.MODE_PRIVATE)
    val dir = File(ctx.filesDir, "models").apply { mkdirs() }

    fun installed(): List<File> =
        dir.listFiles { f -> f.isFile && (f.name.endsWith(".task") || f.name.endsWith(".bin")) }?.sortedBy { it.name } ?: emptyList()

    var active: String?
        get() = prefs.getString("active", null)
        set(v) { prefs.edit().putString("active", v).apply() }

    fun activeFile(): File? = active?.let { File(dir, it) }?.takeIf { it.exists() }

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
class LlmEngine(private val ctx: Context) {
    private var llm: LlmInference? = null
    var loadedName by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    suspend fun load(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            loading = true
            llm?.close(); llm = null; loadedName = null
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(1024)
                .build()
            llm = LlmInference.createFromOptions(ctx, opts)
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
        try {
            val sessionOpts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(0.7f)
                .setRandomSeed(7)
                .build()
            LlmInferenceSession.createFromOptions(engine, sessionOpts).use { session ->
                session.addQueryChunk(prompt)
                session.generateResponse()
            }
        } catch (e: Throwable) {
            null
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
        var t = raw.replace(Regex("<[^>]*>"), "").trim()
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
    suspend fun reply(p: Profile, b: Buddy, hist: List<ChatMsg>, text: String): String {
        if (Safety.isUnsafe(text)) return Safety.redirect(p.name)
        Safety.identityReply(text, b)?.let { return it }
        val model = engine.loadedName
        if (model != null) {
            val out = engine.generate(buildPrompt(model, system(p, b), hist, text))
            Safety.cleanOutput(out ?: "")?.let { return it }
        }
        return Script.reply(p, b, text)
    }

    private fun system(p: Profile, b: Buddy): String {
        val who = when (p.level) {
            1 -> "a 3 year old child. Use very short, very simple words."
            2 -> "a 5 year old child in kindergarten. Use short, simple sentences."
            else -> "a 7 year old child in grade KG-II. Use simple sentences."
        }
        return "You are ${b.name} ${b.title}, a cheerful cartoon friend for ${p.name}, $who " +
            "Reply in English with at most 2 short sentences. Sometimes add one Urdu word with its meaning. " +
            "Help the child learn English, letters, numbers, colors, animals, kindness and fun stories. " +
            "Praise the child. End with one easy question so the child keeps speaking. " +
            "Only talk about things suitable for small children. If asked about anything scary or adult, say it is a question for Mama or Papa and suggest a game. " +
            "Never talk about yourself as a computer, program or model."
    }

    private fun buildPrompt(modelName: String, system: String, hist: List<ChatMsg>, user: String): String {
        val h = hist.takeLast(6)
        return if (modelName.contains("qwen", true) || modelName.contains("chatml", true)) {
            val sb = StringBuilder("<|im_start|>system\n$system<|im_end|>\n")
            h.forEach { sb.append("<|im_start|>${if (it.fromChild) "user" else "assistant"}\n${it.text}<|im_end|>\n") }
            sb.append("<|im_start|>user\n$user<|im_end|>\n<|im_start|>assistant\n").toString()
        } else {
            val sb = StringBuilder("<start_of_turn>user\n$system\n\n")
            if (h.isNotEmpty()) {
                sb.append("Conversation so far:\n")
                h.forEach { sb.append(if (it.fromChild) "Child: " else "Buddy: ").append(it.text).append("\n") }
                sb.append("\n")
            }
            sb.append("Child: $user<end_of_turn>\n<start_of_turn>model\n").toString()
        }
    }
}

/** Works with no model installed: simple, safe, scripted replies. */
object Script {
    fun reply(p: Profile, b: Buddy, text: String): String {
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
            w("your name", "name") -> "My name is ${b.name}. ${b.name}. Can you say ${b.name}?"
            w("joke") -> Content.jokes.random()
            w("story") -> Content.stories.random().let { it.title + ". " + it.lines.joinToString(" ") { l -> l.en } }
            w("word") -> Content.urdu.random().let { "Today's word: ${it.first} (${it.second}) means ${it.third}. Say ${it.third} three times!" }
            w("count", "counting", "numbers") -> {
                val n = if (p.level == 1) 5 else 10
                "Let's count! " + (1..n).joinToString(", ") + ". Now you count to $n!"
            }
            w("color", "colour", "colors") -> "Red like an apple, yellow like the sun, blue like the sky. What is your favorite color?"
            w("animal", "animals") -> "Cats say meow, dogs say woof, cows say moo. What does a duck say?"
            w("thanks", "thank") -> "You are welcome, ${p.name}! You are so polite."
            w("bye", "goodbye") -> "Bye bye ${p.name}! Khuda hafiz! See you soon."
            else -> "That is interesting, ${p.name}! Let's try something fun. Say joke, story, count, or spell cat."
        }
    }
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
