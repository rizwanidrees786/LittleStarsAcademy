package com.rizwan.kidslearn

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SUBJ_EMOJI = mapOf("English" to "🔤", "Maths" to "🔢", "Urdu" to "ا", "Islamiat" to "🕌", "GK & Science" to "🔬", "Life Skills" to "🤝")
private val SUBJ_TITLE = mapOf("English" to "English Book", "Maths" to "Maths Book", "Urdu" to "اردو  ·  Urdu Book", "Islamiat" to "اسلامیات  ·  Islamiat Book",
    "GK & Science" to "General Knowledge and Science", "Life Skills" to "Life Skills and Good Habits")
private val LEVEL_NAME = mapOf(1 to "Play Group", 2 to "KG", 3 to "Class One")

// =============================================================== PIN pad (each child has an own secret PIN)
@Composable
fun PinPad(title: String, subtitle: String, onComplete: (String) -> Unit, onCancel: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    var pin by remember(title) { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color(0xF00A0B2E)).clickable { }, contentAlignment = Alignment.Center) {
        GlassCard(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(subtitle, color = SunYellow, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                repeat(4) { i -> Box(Modifier.size(18.dp).clip(CircleShape).background(if (i < pin.length) Teal else Color.White.copy(alpha = 0.25f))) }
            }
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier.size(68.dp).clip(CircleShape).background(if (k.isEmpty()) Color.Transparent else Color.White.copy(alpha = 0.16f))
                                .clickable(enabled = k.isNotEmpty()) {
                                    if (k == "⌫") pin = pin.dropLast(1)
                                    else if (pin.length < 4) {
                                        pin += k
                                        if (pin.length == 4) { val p = pin; pin = ""; onComplete(p) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(k, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            extra?.invoke()
            Spacer(Modifier.height(6.dp))
            Chip("Cancel", false, Modifier.width(130.dp)) { onCancel() }
        }
    }
}

// =============================================================== Parent extras: Urdu voice check, open all lessons
fun openTtsSettings(ctx: Context) {
    try { ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Throwable) {
        try { ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Throwable) { }
    }
}

@Composable
fun ParentExtras(s: AppState) {
    val ctx = LocalContext.current
    var openAll by remember { mutableStateOf(s.store.openAll()) }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Urdu voice: " + (if (s.speaker.urduAvailable) "installed ✔" else "NOT installed ✖"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Arabic voice (for duas and Quran): " + (if (s.speaker.arabicAvailable) "installed ✔" else "not installed (the Urdu voice is used)"), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("For correct Urdu pronunciation install the Google Urdu (Pakistan) voice in the phone's text-to-speech settings.", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        GradientButton("🔊 Open voice settings") { openTtsSettings(ctx) }
        Spacer(Modifier.height(6.dp))
        GradientButton("🔊 Test Urdu voice", c1 = Color(0xFF00897B), c2 = Teal) { s.speaker.speakAuto("السلام علیکم! میرا نام ${s.buddy.name} ہے۔ آئیے اردو سیکھیں۔") }
    }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Lessons", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(if (openAll) "All lessons are open for the children." else "Lessons unlock one by one, like a school book.", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        GradientButton(if (openAll) "📚 Open all: ON (tap to lock)" else "📚 Unlock one by one: ON (tap to open all)", c1 = Color(0xFFFF6F00), c2 = Color(0xFFFFB300)) { openAll = !openAll; s.store.setOpenAll(openAll) }
    }
}

// =============================================================== Syllabus home: books
@Composable
fun LearnScreen(s: AppState, go: (Screen) -> Unit, onBack: () -> Unit) {
    val profile = s.profile ?: return
    var lv by remember { mutableIntStateOf(profile.level) }
    var all by remember { mutableStateOf(Curriculum.all) }
    LaunchedEffect(Unit) { while (all.isEmpty()) { delay(200); all = Curriculum.all } }
    val done = remember(s.progressVersion, lv) { s.store.doneLessons(profile.id) }
    val books = remember(all, lv) { all.filter { it.level == lv }.groupBy { it.subject } }
    val next = remember(all, done) { all.firstOrNull { it.level == profile.level && !done.containsKey(it.id) } }
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        TopBar("My Syllabus", onBack, "✅ ${done.size}")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..profile.level).forEach { l -> Chip(LEVEL_NAME[l] ?: "Level $l", lv == l, Modifier.weight(1f)) { lv = l } }
        }
        Text(if (lv == profile.level) "${profile.name}'s books for ${LEVEL_NAME[lv]}" else "Earlier books: revise the basics", color = SunYellow, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        if (next != null && lv == profile.level) GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { go(Screen.Lesson(next.id)) }) {
            Text("▶ Continue where you stopped", color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${next.emoji} ${next.title}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(next.subject + "  ·  " + next.unit, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        GradientButton("🎯 Focus on one topic (test preparation)", Modifier.fillMaxWidth().padding(vertical = 4.dp), Color(0xFF00897B), Teal) { go(Screen.Focus) }
        if (all.isEmpty()) Text("Loading books…", color = Color.White)
        books.forEach { (subject, ls) ->
            val d = ls.count { done.containsKey(it.id) }
            GlassCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = { go(Screen.Book(lv, subject)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(SUBJ_EMOJI[subject] ?: "📘", fontSize = 40.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(SUBJ_TITLE[subject] ?: subject, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("$d of ${ls.size} lessons done", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        LinearProgressIndicator(progress = { if (ls.isEmpty()) 0f else d / ls.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(6.dp)), color = Good, trackColor = Color.White.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// =============================================================== One book: units and lessons
@Composable
fun BookScreen(s: AppState, level: Int, subject: String, go: (Screen) -> Unit, onBack: () -> Unit) {
    val profile = s.profile ?: return
    var all by remember { mutableStateOf(Curriculum.all) }
    LaunchedEffect(Unit) { while (all.isEmpty()) { delay(200); all = Curriculum.all } }
    val done = remember(s.progressVersion) { s.store.doneLessons(profile.id) }
    val ls = remember(all) { all.filter { it.level == level && it.subject == subject } }
    val openAll = s.store.openAll() || level < profile.level
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar(SUBJ_TITLE[subject] ?: subject, onBack, "${ls.count { done.containsKey(it.id) }}/${ls.size}")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var lastUnit = ""
            items(ls.size) { idx ->
                val l = ls[idx]
                val open = openAll || idx == 0 || done.containsKey(ls[idx - 1].id)
                val pct = done[l.id]
                Column(Modifier.fillMaxWidth()) {
                    if (idx == 0 || ls[idx - 1].unit != l.unit) Text("📖 ${l.unit}", color = SunYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    GlassCard(Modifier.fillMaxWidth(), onClick = { if (open) go(Screen.Lesson(l.id)) else s.say("Finish the lesson before this one first!") }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(l.emoji, fontSize = 30.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(l.title, color = if (open) Color.White else Color.White.copy(alpha = 0.45f), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(if (pct != null) "✅ $pct%" else if (open) "▶" else "🔒", color = SunYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================== A question runner (used by lessons and by focus practice)
@Composable
fun QuizRunner(s: AppState, qs: List<Question>, header: String, onFinish: (Int) -> Unit) {
    var qi by remember(qs) { mutableIntStateOf(0) }
    var score by remember(qs) { mutableIntStateOf(0) }
    var chosen by remember(qs) { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    val q = qs[qi.coerceAtMost(qs.lastIndex)]
    LaunchedEffect(qi, qs) { s.say(q.speak) }
    LaunchedEffect(chosen) {
        if (chosen != null) {
            delay(1600)
            if (qi + 1 >= qs.size) onFinish(score) else { qi++; chosen = null }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(progress = { (qi + 1) / qs.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)), color = Teal, trackColor = Color.White.copy(alpha = 0.2f))
            Text("$header   ${qi + 1}/${qs.size}   ⭐ $score", color = SunYellow, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
            GlassCard(Modifier.fillMaxWidth().clickable { s.say(q.speak) }) {
                if (q.visual.isNotEmpty()) Text(q.visual, fontSize = 44.sp, textAlign = TextAlign.Center, color = Color.White)
                Text(q.prompt, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            q.options.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { opt ->
                        val bg = when {
                            chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                            opt == q.answer -> Brush.linearGradient(listOf(Good, Good))
                            opt == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                            else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                        }
                        Box(
                            Modifier.weight(1f).padding(6.dp).height(84.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                                .clickable {
                                    if (chosen == null) {
                                        chosen = opt
                                        val ok = opt == q.answer
                                        reward(s, q.skill, q.label, ok)
                                        if (ok) { score++; confetti++; s.say(Content.cheer()) } else s.say("Good try! The answer is ${q.answer}.")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(opt, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== A lesson: teach first, then practise, then unlock the next one
@Composable
fun LessonScreen(s: AppState, id: String, go: (Screen) -> Unit, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val lesson = remember(id) { Curriculum.all.firstOrNull { it.id == id } }
    if (lesson == null) { LaunchedEffect(Unit) { onBack() }; return }
    val scope = rememberCoroutineScope()
    var stage by remember(id) { mutableIntStateOf(0) }
    var page by remember(id) { mutableIntStateOf(0) }
    var qs by remember(id) { mutableStateOf<List<Question>>(emptyList()) }
    var score by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id) { lesson.practice.map { it.level }.distinct().forEach { Bank.load(s.ctx, it) } }
    LaunchedEffect(stage, page) { if (stage == 0) s.say(lesson.pages[page].t) }

    fun startPractice() {
        stage = 1; qs = emptyList()
        scope.launch {
            var list = Bank.lessonQuestions(lesson, 8)
            var tries = 0
            while (list.isEmpty() && tries < 60) { delay(250); list = Bank.lessonQuestions(lesson, 8); tries++ }
            if (list.isEmpty()) { stage = 0 } else qs = list
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar(lesson.title, onBack, if (stage == 0) "${page + 1}/${lesson.pages.size}" else "")
        when (stage) {
            0 -> {
                val pg = lesson.pages[page]
                val big = hasArabic(pg.t)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(lesson.unit, color = SunYellow, fontSize = 14.sp)
                    GlassCard(Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable { s.say(pg.t) }) {
                        if (pg.e.isNotEmpty()) Text(pg.e, fontSize = if (pg.e.length > 8) 40.sp else 76.sp, textAlign = TextAlign.Center, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(plain(pg.t), color = Color.White, fontSize = if (big) 28.sp else 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = if (big) 40.sp else 32.sp)
                    }
                    if (hasArabic(pg.t) && !s.speaker.urduAvailable) Text("Urdu voice is not installed. Ask Papa: Parent Zone → Open voice settings.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("◀ Back", false, Modifier.weight(1f)) { if (page > 0) page-- }
                    GradientButton("🔊", Modifier.width(70.dp)) { s.say(pg.t) }
                    if (page < lesson.pages.size - 1) Chip("Next ▶", true, Modifier.weight(1f)) { page++ }
                    else GradientButton("Start ▶", Modifier.weight(1.3f), Good, Color(0xFF00E676)) { startPractice() }
                }
            }
            1 -> Box(Modifier.weight(1f).fillMaxWidth()) {
                if (qs.isEmpty()) Text("Getting the questions ready…", color = Color.White, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
                else QuizRunner(s, qs, "Practice") { sc ->
                    score = sc
                    val pct = sc * 100 / qs.size
                    if (pct >= 70) s.store.markLesson(profile.id, lesson.id, pct)
                    s.progressVersion++
                    stage = 2
                }
            }
            else -> {
                val pct = if (qs.isEmpty()) 0 else score * 100 / qs.size
                val passed = pct >= 70
                val sib = Curriculum.all.filter { it.level == lesson.level && it.subject == lesson.subject }
                val nxt = sib.getOrNull(sib.indexOfFirst { it.id == lesson.id } + 1)
                LaunchedEffect(stage) { s.say(if (passed) "Well done ${profile.name}! You finished this lesson." else "Good try ${profile.name}. Let's learn it once more.") }
                Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(if (passed) "🏆" else "💪", fontSize = 90.sp)
                    Text("$score / ${qs.size}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (passed) "Lesson complete!" else "You need ${(qs.size * 7 + 9) / 10} right to pass. Try again!", color = SunYellow, fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    if (passed && nxt != null) GradientButton("Next lesson ▶", Modifier.fillMaxWidth()) { go(Screen.Lesson(nxt.id)) }
                    if (!passed) GradientButton("Teach me again", Modifier.fillMaxWidth()) { stage = 0; page = 0 }
                    Spacer(Modifier.height(8.dp))
                    GradientButton("Practise again", Modifier.fillMaxWidth(), Color(0xFF00897B), Teal) { startPractice() }
                    Spacer(Modifier.height(8.dp))
                    Chip("Back to the book", false, Modifier.fillMaxWidth()) { onBack() }
                }
            }
        }
    }
}

// =============================================================== Focus: prepare one or more topics only
@Composable
fun FocusScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    var all by remember { mutableStateOf(Curriculum.all) }
    LaunchedEffect(Unit) {
        for (l in 1..profile.level) Bank.load(s.ctx, l)
        while (all.isEmpty()) { delay(200); all = Curriculum.all }
    }
    var query by remember { mutableStateOf("") }
    val picked = remember { mutableStateListOf<String>() }
    var qs by remember { mutableStateOf<List<Question>>(emptyList()) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shown = remember(all, query) {
        all.filter { it.level <= profile.level && (query.isBlank() || (it.title + " " + it.subject + " " + it.unit).contains(query, true)) }
    }

    fun start() {
        busy = true
        scope.launch {
            val chosen = all.filter { it.id in picked }
            chosen.forEach { l -> l.practice.map { it.level }.distinct().forEach { Bank.load(s.ctx, it) } }
            var tries = 0
            while (tries < 60 && chosen.any { Bank.lessonQuestions(it, 1).isEmpty() }) { delay(250); tries++ }
            val per = maxOf(3, 24 / maxOf(chosen.size, 1))
            val list = ArrayList<Question>()
            chosen.forEach { list += Bank.lessonQuestions(it, per) }
            qs = list.shuffled().take(30)
            busy = false
            if (qs.isNotEmpty()) { result = -1; running = true }
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar("Focus Practice", { if (running) running = false else onBack() })
        if (running) {
            if (result < 0) Box(Modifier.weight(1f).fillMaxWidth()) { QuizRunner(s, qs, "Focus") { result = it } }
            else Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(if (result * 100 / qs.size >= 70) "🏆" else "💪", fontSize = 90.sp)
                Text("$result / ${qs.size}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                GradientButton("Practise these topics again", Modifier.fillMaxWidth()) { start() }
                Spacer(Modifier.height(8.dp))
                Chip("Choose other topics", false, Modifier.fillMaxWidth()) { running = false }
            }
        } else {
            Text("Choose what you want to prepare. Only these topics will be asked.", color = SunYellow, fontSize = 14.sp)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 6.dp), placeholder = { Text("Search: table of 5, letters, Urdu, planets…") }, singleLine = true)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(shown) { l ->
                    val on = l.id in picked
                    GlassCard(Modifier.fillMaxWidth(), onClick = { if (on) picked.remove(l.id) else picked.add(l.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(if (on) "✅" else "⬜", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${l.emoji} ${l.title}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("${l.subject} · ${LEVEL_NAME[l.level]}", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            GradientButton(if (busy) "Getting ready…" else "▶ Start (${picked.size} topics)", Modifier.fillMaxWidth().padding(top = 8.dp), Good, Color(0xFF00E676)) { if (picked.isNotEmpty() && !busy) start() }
        }
    }
}
