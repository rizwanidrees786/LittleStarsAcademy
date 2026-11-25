package com.rizwan.kidslearn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =============================================================== Brain boost 1: patterns (what comes next?)
private class PQ(val seq: String, val answer: String, val options: List<String>, val say: String)

private fun makePattern(style: String, level: Int): PQ {
    if (style == "num") {
        val kinds = when (level) { 1 -> listOf(1, 2); 2 -> listOf(1, 2, 5, 10); else -> listOf(2, 3, 4, 5, 10, -2, 0) }
        val k = kinds.random()
        val start = if (k == -2) (14..24).random() else if (k == 0) listOf(1, 2, 3).random() else (1..8).random()
        val seq = ArrayList<Int>()
        var v = start
        repeat(5) { seq.add(v); v = if (k == 0) v * 2 else v + k }
        val ans = seq.last()
        val shown = seq.dropLast(1).joinToString(",  ") + ",  ?"
        val others = listOf(ans + 1, ans - 1, ans + 2, ans - 2, ans + 3, ans + 10, ans - 10).filter { it != ans && it >= 0 }.shuffled().take(3)
        return PQ(shown, ans.toString(), (listOf(ans) + others).shuffled().map { it.toString() }, "What number comes next?")
    }
    val all = listOf("🔴", "🔵", "🟢", "🟡", "⭐", "🌙", "🍎", "🍌", "🐱", "🐶", "🚗", "🎈")
    val picks = all.shuffled().take(3)
    val unit: List<String> = when (level) {
        1 -> listOf(picks[0], picks[1])
        2 -> listOf(listOf(picks[0], picks[1]), listOf(picks[0], picks[0], picks[1]), listOf(picks[0], picks[1], picks[1])).random()
        else -> listOf(listOf(picks[0], picks[1]), listOf(picks[0], picks[0], picks[1]), listOf(picks[0], picks[1], picks[2])).random()
    }
    val seq = List(unit.size * 3) { unit[it % unit.size] }
    val hidden = unit.size * 2 + 1
    val ans = seq[hidden]
    val opts = (listOf(ans) + all.filter { it != ans }.shuffled().take(3)).shuffled()
    return PQ(seq.take(hidden).joinToString(" ") + " ❓", ans, opts, "What comes next in the pattern?")
}

@Composable
fun PatternGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 8
    var score by remember { mutableIntStateOf(0) }
    var asked by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(makePattern(d.pack, profile.level)) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(asked, done) { if (!done) s.say(q.say) }
    LaunchedEffect(chosen) {
        if (chosen != null) { delay(1700); if (asked + 1 >= goal) done = true else { q = makePattern(d.pack, profile.level); chosen = null; asked++ } }
    }
    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🧩", "Pattern master!", "$score of $goal right  +$score ⭐", { score = 0; asked = 0; done = false; chosen = null; q = makePattern(d.pack, profile.level) }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say(q.say) }) {
                Text(q.seq, color = Color.White, fontSize = if (d.pack == "num") 34.sp else 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("🔊 " + q.say, color = SunYellow, fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            ChoiceGrid(q.options.map { Item(it, it) }, chosen, q.answer, d.pack == "num") { o ->
                chosen = o.name
                val ok = o.name == q.answer
                reward(s, "Patterns", "Pattern ${d.pack}", ok)
                if (ok) { score++; confetti++; s.say(Content.cheer()) } else s.say("The answer is ${q.answer}.")
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Brain boost 2: picture sudoku (every picture once in each row and column)
private fun latin(n: Int): List<IntArray> {
    val perm = (0 until n).shuffled(); val rows = (0 until n).shuffled(); val cols = (0 until n).shuffled()
    return List(n) { r -> IntArray(n) { c -> perm[(rows[r] + cols[c]) % n] } }
}

@Composable
fun SudokuGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val n = if (profile.level >= 3) 4 else 3
    val goal = 3
    val symbols = remember(d.pack) { Packs.items(d.pack).shuffled().take(n) }
    var puzzle by remember { mutableIntStateOf(0) }
    var solved by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val sol = remember(puzzle) { latin(n) }
    val blank = remember(puzzle) {
        val bl = HashSet<Int>()
        for (r in 0 until n) { if (n == 3 && r == 2 && Random.nextBoolean()) continue; bl.add(r * n + Random.nextInt(n)) }
        bl
    }
    val filled = remember(puzzle) { mutableStateListOf<Int>() }
    var sel by remember(puzzle) { mutableIntStateOf(-1) }
    var lives by remember(puzzle) { mutableIntStateOf(3) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(puzzle) { s.say("Each picture must be only once in every row and column!") }

    fun place(sym: Int) {
        if (sel < 0 || failed) return
        val cell = sel
        if (sym == sol[cell / n][cell % n]) {
            filled.add(cell); sel = -1
            reward(s, "Logic", "Sudoku", true)
            s.say(symbols[sym].name)
            if (filled.size == blank.size) {
                confetti++; solved++
                s.say(Content.cheer())
                scope.launch { delay(1600); if (solved >= goal) done = true else puzzle++ }
            }
        } else {
            lives--; reward(s, "Logic", "Sudoku", false); s.say("Not that one. Look at the row and the column!")
            if (lives <= 0) failed = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🧩", "Puzzle master!", "+$goal ⭐", { solved = 0; done = false; puzzle++ }, onBack)
        else if (failed) DoneView("💪", "Good try!", "Let's try a new puzzle", { failed = false; puzzle++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "${"♥".repeat(lives)}  ${solved}/$goal")
            Text("Tap a ❓ box, then choose the missing picture.", color = SunYellow, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            (0 until n).forEach { r ->
                Row(Modifier.fillMaxWidth().height(76.dp)) {
                    (0 until n).forEach { c ->
                        val cell = r * n + c
                        val hidden = cell in blank && cell !in filled
                        val bg = when { hidden && sel == cell -> Color(0xFFFF4FA3); hidden -> Color.White.copy(alpha = 0.18f); cell in filled -> Good; else -> Purple.copy(alpha = 0.6f) }
                        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable { if (hidden) sel = cell }, contentAlignment = Alignment.Center) {
                            Text(if (hidden) "❓" else symbols[sol[r][c]].e, fontSize = 34.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Pictures", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(Modifier.fillMaxWidth()) {
                symbols.forEachIndexed { i, it ->
                    Box(Modifier.weight(1f).height(76.dp).padding(5.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Teal, Sky))).clickable { place(i) }, contentAlignment = Alignment.Center) {
                        Text(it.e, fontSize = 34.sp)
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Brain boost 3: match up (join pictures with their names)
@Composable
fun MatchGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val pool = remember { Packs.items(d.pack) }
    val rounds = 3
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val pairs = remember(round) { pool.shuffled().take(5) }
    val right = remember(round) { pairs.shuffled() }
    val matched = remember(round) { mutableStateListOf<String>() }
    var leftSel by remember(round) { mutableStateOf<String?>(null) }
    var flash by remember(round) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(round) { s.say("Join each picture with its name!") }
    fun label(it: Item) = if (d.pack == "letters") it.name.lowercase() else it.name

    fun pickRight(it: Item) {
        val l = leftSel ?: return
        if (it.name in matched) return
        if (it.name == l) {
            matched.add(l); leftSel = null
            reward(s, "Matching", "Match ${d.pack}", true); s.say(it.name)
            if (matched.size == pairs.size) { confetti++; scope.launch { delay(1300); if (round + 1 >= rounds) done = true else round++ } }
        } else {
            reward(s, "Matching", "Match ${d.pack}", false); flash = it.name
            scope.launch { delay(500); flash = null }
            s.say("Not that one. Try again!")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🔗", "Great matching!", "+${rounds * 5} ⭐", { round = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "Round ${round + 1}/$rounds")
            Text("Tap a picture, then tap its match.", color = SunYellow, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    pairs.forEach { it ->
                        val bg = when { it.name in matched -> Good; leftSel == it.name -> Color(0xFFFF4FA3); else -> Purple.copy(alpha = 0.8f) }
                        Box(Modifier.weight(1f).fillMaxWidth().padding(5.dp).clip(RoundedCornerShape(18.dp)).background(bg).clickable { if (it.name !in matched) { leftSel = it.name; s.say(it.name) } }, contentAlignment = Alignment.Center) {
                            Text(it.e, fontSize = 34.sp, color = Color.White)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    right.forEach { it ->
                        val bg = when { it.name in matched -> Good; flash == it.name -> Bad; else -> Color(0xFF5E35B1).copy(alpha = 0.85f) }
                        Box(Modifier.weight(1f).fillMaxWidth().padding(5.dp).clip(RoundedCornerShape(18.dp)).background(bg).clickable { pickRight(it) }, contentAlignment = Alignment.Center) {
                            Text(label(it), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Brain boost 4: make the number
@Composable
fun MakeGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val target = d.pack.toIntOrNull() ?: 10
    val goal = 8
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val tiles = remember(round) {
        val a = (0..target).random()
        val t = mutableListOf(a, target - a)
        while (t.size < 6) t.add((0..target).random())
        t.shuffled()
    }
    val used = remember(round) { mutableStateListOf<Boolean>().also { l -> repeat(6) { l.add(false) } } }
    var first by remember(round) { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(round, done) { if (!done) s.say("Find two numbers that make $target!") }

    fun tap(i: Int) {
        if (used[i]) return
        if (first == -1) { first = i; s.say(tiles[i].toString()); return }
        if (first == i) { first = -1; return }
        val a = tiles[first]; val b = tiles[i]
        if (a + b == target) {
            used[first] = true; used[i] = true; first = -1
            score++; confetti++
            reward(s, "Addition", "Make $target", true)
            s.say("$a plus $b makes $target!")
            scope.launch { delay(1400); if (score >= goal) done = true else round++ }
        } else {
            reward(s, "Addition", "Make $target", false)
            s.say("$a plus $b is ${a + b}. Try again!")
            first = -1
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("➕", "Number champion!", "+$goal ⭐", { score = 0; done = false; round++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say("Find two numbers that make $target!") }) {
                Text("Make  $target", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                Text("Tap two numbers that add up to $target", color = SunYellow, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
            tiles.indices.toList().chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { i ->
                        val bg = when { used[i] -> Good; first == i -> Color(0xFFFF4FA3); else -> Purple.copy(alpha = 0.85f) }
                        Box(Modifier.weight(1f).padding(6.dp).height(100.dp).alpha(if (used[i]) 0.5f else 1f).clip(RoundedCornerShape(24.dp)).background(bg).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                            Text(tiles[i].toString(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Brain boost 5: quick count (flash and count)
@Composable
fun SubitGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 8
    val maxN = when (profile.level) { 1 -> 5; 2 -> 8; else -> 12 }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var chosen by remember(round) { mutableStateOf<String?>(null) }
    var phase by remember(round) { mutableIntStateOf(0) }
    val n = remember(round) { Random.nextInt(1, maxN + 1) }
    val emoji = remember(round) { if (d.pack == "dots") listOf("🔴", "🔵", "🟢", "🟡", "⭐").random() else Packs.items("animals").random().e }
    val opts = remember(round) { (listOf(n) + (1..maxN + 2).filter { it != n }.shuffled().take(3)).shuffled().map { Item(it.toString(), it.toString()) } }
    LaunchedEffect(round) {
        phase = 0; s.say("Look quickly!")
        delay(1300L + n * 100L)
        phase = 1; s.say("How many did you see?")
    }
    LaunchedEffect(chosen) { if (chosen != null) { delay(1600); if (round + 1 >= goal) done = true else round++ } }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("⚡", "Fast eyes!", "$score of $goal right  +$score ⭐", { round = 0; score = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            GlassCard(Modifier.fillMaxWidth().height(170.dp)) {
                Text(if (phase == 0) emoji.repeat(n) else "❓", fontSize = if (phase == 0) 46.sp else 80.sp, textAlign = TextAlign.Center, color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            if (phase == 1) ChoiceGrid(opts, chosen, n.toString(), true) { o ->
                chosen = o.name
                val ok = o.name == n.toString()
                reward(s, "Counting", "Quick count", ok)
                if (ok) { score++; confetti++; s.say(Content.cheer()) } else s.say("There were $n.")
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Brain boost 6: color magic (the colour of the letters, not the word)
private val STROOP = listOf("Red" to Color(0xFFFF5252), "Blue" to Color(0xFF40C4FF), "Green" to Color(0xFF00E676),
    "Yellow" to Color(0xFFFFD54F), "Purple" to Color(0xFFBA68C8), "Orange" to Color(0xFFFF9800))
private class SQ(val word: String, val ink: Int, val options: List<String>)
private fun newStroop(): SQ {
    val w = STROOP.indices.random()
    val ink = if (Random.nextInt(100) < 75) (STROOP.indices.filter { it != w }).random() else w
    val opts = (listOf(STROOP[ink].first) + STROOP.map { it.first }.filter { it != STROOP[ink].first }.shuffled().take(3)).shuffled()
    return SQ(STROOP[w].first, ink, opts)
}

@Composable
fun StroopGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val goal = 10
    var q by remember { mutableStateOf(newStroop()) }
    var asked by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(done) { if (!done) s.say("Tap the color of the letters, not the word!") }
    LaunchedEffect(chosen) { if (chosen != null) { delay(1300); if (asked + 1 >= goal) done = true else { q = newStroop(); chosen = null; asked++ } } }
    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🎨", "Color wizard!", "$score of $goal right  +$score ⭐", { score = 0; asked = 0; done = false; chosen = null; q = newStroop() }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            Text("Tap the COLOR of the letters, not the word!", color = SunYellow, fontSize = 16.sp, textAlign = TextAlign.Center)
            GlassCard(Modifier.fillMaxWidth().padding(vertical = 14.dp).height(150.dp)) {
                Text(q.word.uppercase(), color = STROOP[q.ink].second, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold)
            }
            ChoiceGrid(q.options.map { Item(it, it) }, chosen, STROOP[q.ink].first, true) { o ->
                chosen = o.name
                val ok = o.name == STROOP[q.ink].first
                reward(s, "Focus", "Color magic", ok)
                if (ok) { score++; confetti++; s.say(Content.cheer()) } else s.say("It was ${STROOP[q.ink].first}.")
            }
        }
        Confetti(confetti)
    }
}
