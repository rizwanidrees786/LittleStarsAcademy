package com.rizwan.kidslearn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Every game feeds the child's progress (stars + weak spots), saved under the child's name. */
fun reward(s: AppState, skill: String, label: String, ok: Boolean) {
    val p = s.profile ?: return
    val prog = s.store.progress(p.id)
    prog.record(Question(skill, label, "", "", "", emptyList(), ""), ok)
    if (ok) prog.stars++
    s.store.saveProgress(p.id, prog)
    s.progressVersion++
}

@Composable
fun DoneView(emoji: String, title: String, sub: String, again: () -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 90.sp)
        Text(title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(sub, color = SunYellow, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        GradientButton("Play again", Modifier.fillMaxWidth(), onClick = again)
        Spacer(Modifier.height(12.dp))
        GradientButton("All games", Modifier.fillMaxWidth(), Color(0xFF00897B), Teal, onClick = back)
    }
}

// =============================================================== Games hub
@Composable
fun GamesScreen(s: AppState, go: (Screen) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        TopBar("Play Games", onBack)
        data class G(val e: String, val t: String, val sub: String, val a: Color, val b: Color, val to: Screen)
        val games = listOf(
            G("🎈", "Balloon Pop", "Pop the right one", Pink, Color(0xFFFF7043), Screen.Balloons),
            G("🧠", "Memory Match", "Find the pairs", Purple, Sky, Screen.Memory),
            G("🎨", "Magic Paint", "Draw with fingers", Color(0xFF00897B), Teal, Screen.Paint),
            G("🐮", "Sound Safari", "Who says moo?", Color(0xFFFF6F00), Color(0xFFFFB300), Screen.Safari),
            G("⭐", "Sticker Book", "Collect them all", Color(0xFF1565C0), Sky, Screen.Stickers),
            G("🐝", "Spelling Bee", "Tap letters in order", Color(0xFFF9A825), Color(0xFFFFEE58), Screen.Spell),
            G("🔴", "Simon Says", "Remember the colors", Color(0xFFD32F2F), Color(0xFFFF8A65), Screen.Simon),
            G("🔨", "Whack-a-Buddy", "Tap the named friend", Color(0xFF6A1B9A), Color(0xFFBA68C8), Screen.Whack),
            G("🔍", "Odd One Out", "Which is different?", Color(0xFF00695C), Color(0xFF4DB6AC), Screen.Odd),
            G("📸", "Photo Friend", "Your face on a hero", Color(0xFFAD1457), Color(0xFFF06292), Screen.PhotoFriend),
        )
        games.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { g -> Tile(g.e, g.t, g.sub, g.a, g.b, Modifier.weight(1f)) { go(g.to) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Every correct tap earns a ⭐. Stars unlock stickers!", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

// =============================================================== Balloon Pop
private data class Balloon(val id: Int, val label: String, val x: Float, val y: Float, val speed: Float, val color: Color)
private data class Round(val prompt: String, val answer: String, val labels: List<String>, val skill: String, val tag: String)

private fun makeRound(level: Int): Round = when (level) {
    1 -> {
        val c = ('A'..'Z').random().toString()
        val ls = (('A'..'Z').map { it.toString() } - c).shuffled().take(4) + c
        Round("Pop the letter $c!", c, ls.shuffled(), "Letters", "Letter $c")
    }
    2 -> {
        val n = (1..20).random()
        val ls = ((1..20).toList() - n).shuffled().take(4).map { it.toString() } + n.toString()
        Round("Pop the number $n!", n.toString(), ls.shuffled(), "Numbers", "Number $n")
    }
    else -> {
        val a = (1..10).random(); val b = (1..10).random(); val r = a + b
        val ls = (r - 4..r + 4).filter { it != r && it >= 0 }.shuffled().take(4).map { it.toString() } + r.toString()
        Round("Pop $a plus $b!", r.toString(), ls.shuffled(), "Addition", "Add $a+$b")
    }
}

@Composable
fun BalloonScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 8
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var cur by remember { mutableStateOf(makeRound(profile.level)) }
    val balloons = remember { mutableStateListOf<Balloon>() }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val colors = listOf(Pink, Teal, SunYellow, Sky, Purple, Color(0xFFFF7043))

    LaunchedEffect(round) {
        balloons.clear()
        cur.labels.forEachIndexed { i, l ->
            balloons.add(Balloon(i, l, i * 0.22f + Random.nextFloat() * 0.04f, 0.9f + Random.nextFloat() * 0.6f, 0.10f + Random.nextFloat() * 0.10f, colors[i % colors.size]))
        }
        s.say(cur.prompt)
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            for (i in balloons.indices) {
                val b = balloons[i]
                val ny = b.y - b.speed * dt
                balloons[i] = b.copy(y = if (ny < -0.2f) 1.1f else ny)
            }
        }
    }

    fun pop(b: Balloon) {
        if (done) return
        if (b.label == cur.answer) {
            reward(s, cur.skill, cur.tag, true)
            score++; confetti++
            s.say(Content.cheer())
            if (score >= goal) { done = true; s.say("You popped $goal balloons, ${profile.name}!") }
            else { cur = makeRound(profile.level); round++ }
        } else {
            reward(s, cur.skill, cur.tag, false)
            s.say("That is ${b.label}. Find ${cur.answer}!")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) {
            DoneView("🎈", "Great job!", "You popped $goal balloons  +$goal ⭐", { score = 0; done = false; cur = makeRound(profile.level); round++ }, onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                TopBar("Balloon Pop", onBack, "⭐ $score/$goal")
                GlassCard(Modifier.fillMaxWidth().clickable { s.say(cur.prompt) }) {
                    Text("🔊 " + cur.prompt, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
                val bw = with(LocalDensity.current) { 84.dp.roundToPx() }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val w = constraints.maxWidth
                    val h = constraints.maxHeight
                    balloons.forEach { b ->
                        key(b.id) {
                            Box(
                                Modifier.offset { IntOffset((b.x * (w - bw)).toInt(), (b.y * h).toInt()) }
                                    .size(84.dp, 104.dp).clip(RoundedCornerShape(50))
                                    .background(Brush.verticalGradient(listOf(b.color, b.color.copy(alpha = 0.65f))))
                                    .clickable { pop(b) },
                                contentAlignment = Alignment.Center
                            ) { Text(b.label, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Memory Match
private data class MCard(val key: String, val emoji: String, val up: Boolean = false, val done: Boolean = false)
private val MEM = listOf("🐱" to "Cat", "🐶" to "Dog", "🐰" to "Rabbit", "🦁" to "Lion", "🐘" to "Elephant", "🐵" to "Monkey",
    "🐸" to "Frog", "🐼" to "Panda", "🍎" to "Apple", "🍌" to "Banana", "🍇" to "Grapes", "🍓" to "Strawberry",
    "🚗" to "Car", "🚌" to "Bus", "✈️" to "Airplane", "⚽" to "Ball", "☀️" to "Sun", "🌙" to "Moon", "⭐" to "Star", "🐟" to "Fish")

@Composable
private fun FlipCard(c: MCard, modifier: Modifier, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (c.up || c.done) 180f else 0f, tween(350), label = "flip")
    val front = rot > 90f
    Box(
        modifier.graphicsLayer { rotationY = rot; cameraDistance = 14f * density }
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    c.done -> Brush.linearGradient(listOf(Good, Color(0xFF00E676)))
                    front -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f)))
                    else -> Brush.linearGradient(listOf(Purple, Pink))
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (front) Box(Modifier.graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.emoji, fontSize = 38.sp)
                Text(c.key, color = Color.White, fontSize = 11.sp)
            }
        } else Text("❓", fontSize = 32.sp)
    }
}

@Composable
fun MemoryScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val pairs = if (profile.level >= 3) 8 else 6
    val cols = if (pairs == 8) 4 else 3
    var game by remember { mutableIntStateOf(0) }
    val cards = remember(game) {
        mutableStateListOf<MCard>().also { l ->
            MEM.shuffled().take(pairs).forEach { (e, n) -> l.add(MCard(n, e)); l.add(MCard(n, e)) }
            l.shuffle()
        }
    }
    var first by remember(game) { mutableIntStateOf(-1) }
    var busy by remember(game) { mutableStateOf(false) }
    var moves by remember(game) { mutableIntStateOf(0) }
    var found by remember(game) { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun tap(i: Int) {
        if (busy || cards[i].up || cards[i].done) return
        cards[i] = cards[i].copy(up = true)
        if (first == -1) { first = i; return }
        val a = first
        first = -1
        moves++
        if (cards[a].key == cards[i].key) {
            cards[a] = cards[a].copy(done = true); cards[i] = cards[i].copy(done = true)
            found++; confetti++
            reward(s, "Memory", "Match ${cards[i].key}", true)
            s.say("${cards[i].key}!")
            if (found == pairs) s.say("You found all the pairs in $moves moves, ${profile.name}!")
        } else {
            busy = true
            scope.launch {
                delay(850)
                cards[a] = cards[a].copy(up = false); cards[i] = cards[i].copy(up = false)
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (found == pairs) {
            DoneView("🧠", "All pairs found!", "$moves moves  +$pairs ⭐", { game++ }, onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                TopBar("Memory Match", onBack, "Moves $moves")
                Text("Flip two cards. Find the same picture!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                cards.indices.toList().chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { i -> FlipCard(cards[i], Modifier.weight(1f).fillMaxHeight().padding(4.dp)) { tap(i) } }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Magic Paint
private data class Ink(val color: Color, val width: Float, val pts: List<Offset>)

@Composable
fun PaintScreen(s: AppState, onBack: () -> Unit) {
    val strokes = remember { mutableStateListOf<Ink>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var color by remember { mutableStateOf(Pink) }
    var width by remember { mutableFloatStateOf(16f) }
    val ideas = listOf("Draw a sun ☀️", "Draw a house 🏠", "Draw a fish 🐟", "Draw a flower 🌸", "Draw a car 🚗", "Draw your family 👪", "Draw a rainbow 🌈", "Draw a cat 🐱", "Draw a tree 🌳")
    var idea by remember { mutableStateOf(ideas.random()) }
    val palette = listOf(Pink, Color(0xFFFF5252), Color(0xFFFF9800), SunYellow, Good, Teal, Sky, Purple, Color(0xFF8D6E63), Color.Black, Color.White)

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar("Magic Paint", onBack)
        Text(idea, color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.94f))) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(color, width) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> change.consume(); current = current + change.position },
                        onDragEnd = { if (current.isNotEmpty()) strokes.add(Ink(color, width, current)); current = emptyList() },
                        onDragCancel = { current = emptyList() }
                    )
                }
            ) {
                fun draw(i: Ink) {
                    if (i.pts.size == 1) drawCircle(i.color, i.width / 2, i.pts[0])
                    else {
                        val p = Path()
                        p.moveTo(i.pts[0].x, i.pts[0].y)
                        for (k in 1 until i.pts.size) p.lineTo(i.pts[k].x, i.pts[k].y)
                        drawPath(p, i.color, style = Stroke(i.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                strokes.forEach { draw(it) }
                if (current.isNotEmpty()) draw(Ink(color, width, current))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            palette.forEach { c ->
                Box(Modifier.size(42.dp).clip(CircleShape).background(c).border(if (c == color) 4.dp else 1.dp, if (c == color) Color.White else Color.Gray, CircleShape).clickable { color = c })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Thin", width == 8f, Modifier.width(76.dp)) { width = 8f }
            Chip("Medium", width == 16f, Modifier.width(90.dp)) { width = 16f }
            Chip("Thick", width == 32f, Modifier.width(76.dp)) { width = 32f }
            Chip("↩ Undo", false, Modifier.width(86.dp)) { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }
            Chip("🗑 Clear", false, Modifier.width(90.dp)) { strokes.clear(); current = emptyList() }
            Chip("💡 Idea", false, Modifier.width(86.dp)) { idea = ideas.random(); s.say(idea.filter { it.code < 128 }) }
        }
    }
}

// =============================================================== Sound Safari
private val SAFARI = listOf(
    Triple("🐱", "cat", "Meow meow"), Triple("🐶", "dog", "Woof woof"), Triple("🐮", "cow", "Moo moo"), Triple("🐑", "sheep", "Baa baa"),
    Triple("🦆", "duck", "Quack quack"), Triple("🐔", "hen", "Cluck cluck"), Triple("🐴", "horse", "Neigh neigh"), Triple("🦁", "lion", "Roar"),
    Triple("🐷", "pig", "Oink oink"), Triple("🐸", "frog", "Ribbit ribbit"), Triple("🐝", "bee", "Buzz buzz"), Triple("🐍", "snake", "Hiss hiss"),
    Triple("🦉", "owl", "Hoot hoot"), Triple("🐘", "elephant", "Toot toot")
)

@Composable
fun SafariScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 8
    var score by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(SAFARI.random()) }
    var options by remember { mutableStateOf(SAFARI.shuffled().take(4)) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    fun newRound() {
        val t = SAFARI.random()
        target = t
        options = (SAFARI.filter { it.second != t.second }.shuffled().take(3) + t).shuffled()
        chosen = null
        tick++
    }
    LaunchedEffect(tick) { s.say("${target.third}! Who says that?") }
    LaunchedEffect(chosen) {
        if (chosen != null) { delay(1700); if (score >= goal) done = true else newRound() }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) {
            DoneView("🦁", "Safari complete!", "+$goal ⭐  Well done ${profile.name}!", { score = 0; done = false; newRound() }, onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TopBar("Sound Safari", onBack, "⭐ $score/$goal")
                GlassCard(Modifier.fillMaxWidth().clickable { s.say("${target.third}! Who says that?") }) {
                    Text("🔊", fontSize = 44.sp)
                    Text("\"${target.third}\"", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Who says this?", color = SunYellow, fontSize = 18.sp)
                }
                Spacer(Modifier.height(14.dp))
                options.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { o ->
                            val bg = when {
                                chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                                o.second == target.second -> Brush.linearGradient(listOf(Good, Good))
                                o.second == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                                else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                            }
                            Column(
                                Modifier.weight(1f).padding(6.dp).height(130.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                                    .clickable {
                                        if (chosen == null) {
                                            chosen = o.second
                                            if (o.second == target.second) {
                                                score++; confetti++
                                                reward(s, "Animals", "Sound ${target.second}", true)
                                                s.say("${Content.cheer()} A ${o.second} says ${o.third}!")
                                            } else {
                                                reward(s, "Animals", "Sound ${target.second}", false)
                                                s.say("That is a ${o.second}. A ${target.second} says ${target.third}.")
                                            }
                                        }
                                    },
                                Arrangement.Center, Alignment.CenterHorizontally
                            ) {
                                Text(o.first, fontSize = 54.sp)
                                Text(o.second.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Sticker Book (stars unlock stickers)
private val STICKERS = listOf("🦄", "🐉", "🚀", "🎈", "🍭", "🧁", "🏆", "🎸", "🌟", "🦋", "🐬", "🦖", "🎠", "🏰", "🚁", "🍦", "🎮", "🧸", "🛸", "🌈",
    "🍩", "🦊", "🐼", "🎨", "⚽", "🏀", "🎯", "🎪", "🎡", "🚂", "🐢", "🦜", "🦚", "🦒", "🐯", "🍉", "🍓", "🌻", "🌺", "🎂")

@Composable
fun StickerScreen(s: AppState, onBack: () -> Unit) {
    val p = s.profile ?: return
    val stars = remember(s.progressVersion) { s.store.progress(p.id).stars }
    val perSticker = 3
    val unlocked = minOf(stars / perSticker, STICKERS.size)
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        TopBar("${p.name}'s Stickers", onBack, "⭐ $stars")
        Text(
            if (unlocked >= STICKERS.size) "Wow! You collected ALL stickers!" else "${STICKERS.size - unlocked} stickers to go. Next sticker in ${perSticker - stars % perSticker} ⭐",
            color = SunYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)
        )
        STICKERS.chunked(5).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { c, e ->
                    val idx = r * 5 + c
                    val on = idx < unlocked
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (on) Brush.linearGradient(listOf(Purple.copy(alpha = 0.8f), Pink.copy(alpha = 0.8f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))))
                            .clickable { if (on) s.say("Wow! A beautiful sticker!") else s.say("Keep playing to win this sticker!") },
                        contentAlignment = Alignment.Center
                    ) { Text(if (on) e else "🔒", fontSize = if (on) 30.sp else 22.sp) }
                }
            }
        }
    }
}
