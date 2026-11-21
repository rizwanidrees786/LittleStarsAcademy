package com.rizwan.kidslearn

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ---------------------------------------------------------------- Splash
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val fade = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        fade.animateTo(1f, tween(1200))
        delay(1500)
        onDone()
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.scale(scale.value * glow).size(190.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Purple, Pink, SunYellow))),
                contentAlignment = Alignment.Center
            ) { Text("🌟", fontSize = 96.sp) }
            Spacer(Modifier.height(28.dp))
            Text("Little Stars Academy", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.alpha(fade.value))
            Text("Seekho  ·  Khelo  ·  Bolo", color = SunYellow, fontSize = 18.sp, modifier = Modifier.alpha(fade.value).padding(top = 6.dp))
        }
        Text("Developed by Rizwan Idrees", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).alpha(fade.value))
    }
}

// ---------------------------------------------------------------- Who is learning
@Composable
fun ProfileScreen(s: AppState, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Text("Who is learning today?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Aaj kaun seekhe ga?", color = SunYellow, fontSize = 17.sp)
        Spacer(Modifier.height(14.dp))
        PROFILES.forEach { p ->
            val b = buddyById(s.store.buddyId(p))
            GlassCard(Modifier.fillMaxWidth().padding(vertical = 8.dp), onClick = {
                s.selectProfile(p)
                s.say("Hello ${p.name}! I am ${s.buddy.name}. Let's learn and play!")
                onPick()
            }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    BuddyAvatar(b, 76.dp)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(p.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(p.grade, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Developed by Rizwan Idrees", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------- Choose character
@Composable
fun BuddyPickerScreen(s: AppState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("Choose your friend", onBack)
        Text("Pick any friend. ${s.profile?.name}'s learning stays saved!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BUDDIES) { b ->
                val sel = s.buddy.id == b.id
                GlassCard(Modifier.fillMaxWidth(), onClick = {
                    s.chooseBuddy(b)
                    s.say("Hi! My name is ${b.name}. ${b.name}. Can you say ${b.name}? I love learning with you!")
                }) {
                    BuddyAvatar(b, 62.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(b.name, color = if (sel) SunYellow else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                    Text(if (sel) "✓ selected" else b.title, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Home
@Composable
fun HomeScreen(s: AppState, go: (Screen) -> Unit, onSwitch: () -> Unit) {
    val p = s.profile ?: return
    val stars = remember(s.progressVersion) { s.store.progress(p.id).stars }
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip("← Switch", false, Modifier.width(100.dp)) { onSwitch() }
            Spacer(Modifier.weight(1f))
            Text("⭐ $stars", color = SunYellow, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        BuddyAvatar(s.buddy, 130.dp, Modifier.clickable { s.say("Hi ${p.name}! I am ${s.buddy.name}. What shall we do today?") })
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Hi ${p.name}! I am ${s.buddy.name} ${s.buddy.title}.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("What shall we do today?  Aaj kya karein?", color = SunYellow, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        data class T(val e: String, val t: String, val sub: String, val a: Color, val b: Color, val act: () -> Unit)
        val tiles = listOf(
            T("🎮", "Learn & Play", "Letters, numbers, words", Purple, Pink) { go(Screen.Quiz(false)) },
            T("📖", "Stories & Fun", "Kahani, jokes, riddles", Color(0xFFFF6F00), Color(0xFFFFB300)) { go(Screen.Fun) },
            T("📝", "My Test", "10 questions", Color(0xFF00897B), Teal) { go(Screen.Quiz(true)) },
            T("📈", "My Progress", "Stars & weak spots", Color(0xFF1565C0), Sky) { go(Screen.Progress) },
            T("🧸", "Change Friend", "24 characters", Color(0xFFD81B60), Color(0xFFFF80AB)) { go(Screen.Buddies) },
            T("🔒", "Parent Zone", "PIN protected", Color(0xFF455A64), Color(0xFF90A4AE)) { go(Screen.Parent) },
            T("🗣️", "Speak & Practice", "Say it, win stars", Color(0xFFE65100), Color(0xFFFFB74D)) { go(Screen.Speak) },
            T("💬", "Chat with me", "Talk & learn", Color(0xFF512DA8), Color(0xFFB388FF)) { go(Screen.Chat) },
        )
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { t -> Tile(t.e, t.t, t.sub, t.a, t.b, Modifier.weight(1f), t.act) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Developed by Rizwan Idrees", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------- Quiz / Test
@Composable
fun QuizScreen(s: AppState, test: Boolean, onExit: () -> Unit, onRestart: () -> Unit) {
    val profile = s.profile ?: return
    val progress = remember { s.store.progress(profile.id) }
    val total = 10
    var idx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(Content.next(profile.level, progress)) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var newStars by remember { mutableIntStateOf(0) }
    val retry = remember { mutableListOf<Question>() }

    fun save() { s.store.saveProgress(profile.id, progress); s.progressVersion++ }

    fun answer(opt: String) {
        if (chosen != null) return
        chosen = opt
        val ok = opt == q.answer
        progress.record(q, ok)
        if (ok) {
            score++
            if (!test) { progress.stars++; newStars++ }
            confetti++
            s.say(Content.cheer())
        } else {
            if (!test) retry.add(q)
            s.say("Good try! The answer is ${q.answer}.")
        }
        save()
    }

    fun next() {
        if (idx + 1 >= total) {
            if (test) progress.tests.add(System.currentTimeMillis() to score)
            save()
            done = true
            s.say(if (score >= 8) "Amazing ${profile.name}! You are a champion!" else "Good work ${profile.name}! Let's practice more.")
            return
        }
        idx++
        chosen = null
        q = if (!test && retry.isNotEmpty() && idx >= 2 && Random.nextInt(3) == 0) retry.removeAt(0)
        else Content.next(profile.level, progress)
    }

    LaunchedEffect(idx, q) { s.say(q.speak) }
    LaunchedEffect(chosen) {
        if (chosen != null && !done) { delay(if (chosen == q.answer) 1800 else 3000); next() }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) {
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(if (score >= 8) "🏆" else if (score >= 5) "🎉" else "💪", fontSize = 90.sp)
                Text("$score / $total", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (test) "Test finished, ${profile.name}!" else "+$newStars ⭐ earned!", color = SunYellow, fontSize = 22.sp)
                Spacer(Modifier.height(24.dp))
                GradientButton(if (test) "Take test again" else "Play again", Modifier.fillMaxWidth(), onClick = onRestart)
                Spacer(Modifier.height(12.dp))
                GradientButton("Home", Modifier.fillMaxWidth(), Color(0xFF00897B), Teal, onClick = onExit)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip("✕", false, Modifier.width(56.dp)) { onExit() }
                    Spacer(Modifier.width(12.dp))
                    LinearProgressIndicator(progress = { (idx + 1) / total.toFloat() }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(8.dp)), color = Teal, trackColor = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.width(12.dp))
                    Text("⭐ $score", color = SunYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                BuddyAvatar(s.buddy, 84.dp, Modifier.clickable { s.say(q.speak) })
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(q.skill + if (test) "  ·  Test ${idx + 1}/$total" else "", color = Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(q.visual, fontSize = 46.sp, textAlign = TextAlign.Center, color = Color.White)
                    Text(q.prompt, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                q.options.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { opt ->
                            val bg = when {
                                chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                                opt == q.answer -> Brush.linearGradient(listOf(Good, Good))
                                opt == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                                else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                            }
                            Box(Modifier.weight(1f).padding(6.dp).height(88.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable { answer(opt) }, contentAlignment = Alignment.Center) {
                                Text(opt, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// ---------------------------------------------------------------- Stories, jokes, riddles
@Composable
fun FunScreen(s: AppState, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var story by remember { mutableStateOf<Story?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("Stories & Fun", onBack = { if (story != null) story = null else onBack() })
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("📖 Stories", "😂 Jokes", "🧩 Riddles").forEachIndexed { i, t ->
                Chip(t, tab == i, Modifier.weight(1f)) { tab = i; story = null }
            }
        }
        when (tab) {
            0 -> if (story == null) Column(Modifier.verticalScroll(rememberScrollState())) {
                Content.stories.forEach { st ->
                    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = { story = st }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(st.emoji, fontSize = 40.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(st.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else StoryReader(s, story!!)
            1 -> JokeView(s)
            else -> RiddleView(s)
        }
    }
}

@Composable
fun StoryReader(s: AppState, st: Story) {
    var i by remember(st) { mutableIntStateOf(0) }
    val line = st.lines[i]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(st.emoji, fontSize = 72.sp)
        Text(st.title, color = SunYellow, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(line.en, color = Color.White, fontSize = 24.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(line.ur, color = SunYellow, fontSize = 26.sp, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("🔊 English") { s.say(line.en) }
            GradientButton("🔊 اردو", c1 = Color(0xFF00897B), c2 = Teal) { s.speaker.speakUr(line.ur) }
        }
        if (!s.speaker.urduAvailable) Text("Urdu voice not installed on this phone: Settings → Language → Text-to-speech → Google → install Urdu voice data.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (i > 0) Chip("◀ Back", false, Modifier.width(90.dp)) { i-- }
            Text("${i + 1}/${st.lines.size}", color = Color.White, fontSize = 16.sp)
            if (i < st.lines.size - 1) Chip("Next ▶", true, Modifier.width(90.dp)) { i++ } else Text("The End 🎉", color = SunYellow, fontSize = 18.sp)
        }
    }
}

@Composable
fun JokeView(s: AppState) {
    var i by remember { mutableIntStateOf(Random.nextInt(Content.jokes.size)) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😂", fontSize = 70.sp)
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(Content.jokes[i], color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("🔊 Tell me!") { s.say(Content.jokes[i]) }
            GradientButton("Next joke", c1 = Color(0xFFFF6F00), c2 = Color(0xFFFFB300)) { i = (i + 1) % Content.jokes.size }
        }
    }
}

@Composable
fun RiddleView(s: AppState) {
    var i by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    val (r, a) = Content.riddles[i]
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🧩", fontSize = 70.sp)
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(r, color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center)
            if (show) { Spacer(Modifier.height(10.dp)); Text(a, color = SunYellow, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("🔊 Read") { s.say(r) }
            GradientButton(if (show) "Next" else "Answer", c1 = Color(0xFF00897B), c2 = Teal) {
                if (show) { i = (i + 1) % Content.riddles.size; show = false } else { show = true; s.say(a) }
            }
        }
    }
}

// ---------------------------------------------------------------- Progress
@Composable
fun ProgressBlock(profile: Profile, p: Progress) {
    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("${profile.name}  ·  ⭐ ${p.stars}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${p.totalCorrect()} correct of ${p.totalAttempts()} answers", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        (Content.skillsFor(profile.level) + SPEAK).forEach { sk ->
            val a = p.attempts[sk] ?: 0; val c = p.correct[sk] ?: 0
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row { Text(sk, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text(if (a == 0) "new" else "$c/$a", color = SunYellow, fontSize = 14.sp) }
                LinearProgressIndicator(progress = { if (a == 0) 0f else c / a.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)), color = if (a > 0 && c / a.toFloat() < 0.6f) Bad else Good, trackColor = Color.White.copy(alpha = 0.2f))
            }
        }
        val weak = p.misses.entries.sortedByDescending { it.value }.take(6)
        Spacer(Modifier.height(8.dp))
        Text("Needs practice", color = Pink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(if (weak.isEmpty()) "Nothing yet. Great!" else weak.joinToString("   ") { "${it.key} (${it.value})" }, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (p.tests.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Recent tests", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(p.tests.takeLast(5).reversed().joinToString("   ") { "${fmt.format(Date(it.first))}: ${it.second}/10" }, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ProgressScreen(s: AppState, onBack: () -> Unit) {
    val p = s.profile ?: return
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("My Progress", onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) { ProgressBlock(p, s.store.progress(p.id)) }
    }
}

// ---------------------------------------------------------------- Parent zone
@Composable
fun ParentScreen(s: AppState, onBack: () -> Unit, onModels: () -> Unit) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("Parent Zone", onBack)
        if (!unlocked) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Enter parent PIN (default 1234)", color = Color.White, fontSize = 18.sp)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6); if (pin == s.store.pin()) unlocked = true },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PROFILES.forEach { pr ->
                    val prog = remember(tick) { s.store.progress(pr.id) }
                    ProgressBlock(pr, prog)
                    GradientButton(if (confirm == pr.id) "Tap again to confirm reset" else "Reset ${pr.name}'s progress", Modifier.fillMaxWidth(), Bad, Color(0xFFB71C1C)) {
                        if (confirm == pr.id) { s.store.resetProgress(pr.id); confirm = null; tick++; s.progressVersion++ } else confirm = pr.id
                    }
                }
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Change PIN", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(6) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Spacer(Modifier.height(6.dp))
                    GradientButton("Save PIN") { if (newPin.length >= 4) { s.store.setPin(newPin); newPin = "" } }
                }
                Spacer(Modifier.height(10.dp))
                GradientButton("🧠 Manage AI models", Modifier.fillMaxWidth(), Teal, Sky, onClick = onModels)
            }
        }
    }
}
