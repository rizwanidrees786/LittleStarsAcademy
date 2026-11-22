package com.rizwan.kidslearn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =============================================================== Spelling Bee
private val SPELL = mapOf(
    1 to listOf("CAT" to "🐱", "DOG" to "🐶", "SUN" to "☀️", "BUS" to "🚌", "HAT" to "🎩", "PIG" to "🐷", "BED" to "🛏️", "BAT" to "🦇", "HEN" to "🐔", "NET" to "🥅"),
    2 to listOf("FISH" to "🐟", "FROG" to "🐸", "DUCK" to "🦆", "BIRD" to "🐦", "MOON" to "🌙", "STAR" to "⭐", "TREE" to "🌳", "BOOK" to "📖", "CAKE" to "🎂", "BALL" to "⚽", "LION" to "🦁", "CORN" to "🌽"),
    3 to listOf("APPLE" to "🍎", "TIGER" to "🐯", "ZEBRA" to "🦓", "HORSE" to "🐴", "PIZZA" to "🍕", "ROBOT" to "🤖", "GRAPES" to "🍇", "BANANA" to "🍌", "ORANGE" to "🍊", "FLOWER" to "🌸", "MONKEY" to "🐵", "RABBIT" to "🐰")
)

@Composable
fun SpellScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 6
    val pool = remember { (SPELL[profile.level] ?: SPELL.getValue(1)).shuffled() }
    var round by remember { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val (word, emoji) = pool[round % pool.size]
    val tiles = remember(round) {
        var t = word.toList().shuffled()
        var tries = 0
        while (t == word.toList() && tries < 10) { t = word.toList().shuffled(); tries++ }
        t
    }
    val used = remember(round) { mutableStateListOf<Boolean>().also { l -> repeat(word.length) { l.add(false) } } }
    var pos by remember(round) { mutableIntStateOf(0) }

    LaunchedEffect(round, done) { if (!done) s.say("Spell ${word.lowercase()}. ${word.lowercase()}.") }

    fun tap(i: Int) {
        if (used[i] || pos >= word.length) return
        if (tiles[i] == word[pos]) {
            used[i] = true; pos++
            s.say(tiles[i].toString())
            if (pos == word.length) {
                confetti++
                reward(s, "Spelling", "Spell ${word.lowercase()}", true)
                s.say(word.toList().joinToString(", ") + ". " + word.lowercase() + "! " + Content.cheer())
                scope.launch { delay(2800); if (round + 1 >= goal) done = true else round++ }
            }
        } else {
            reward(s, "Spelling", "Spell ${word.lowercase()}", false)
            s.say("Try another letter.")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🐝", "Spelling champion!", "+$goal ⭐", { round = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("Spelling Bee", onBack, "⭐ $round/$goal")
            Text(emoji, fontSize = 90.sp, modifier = Modifier.clickable { s.say("Spell ${word.lowercase()}") })
            Text("Tap the letters in order", color = SunYellow, fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                word.forEachIndexed { i, c ->
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (i < pos) Good else Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        if (i < pos) Text(c.toString(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tiles.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(48.dp).alpha(if (used[i]) 0.25f else 1f).clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Purple, Pink))).clickable { tap(i) },
                        contentAlignment = Alignment.Center
                    ) { Text(c.toString(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Simon Says (colour memory)
private val SIMON_COL = listOf(Color(0xFFFF5252), Color(0xFF40C4FF), Color(0xFF00E676), Color(0xFFFFD54F))
private val SIMON_NAME = listOf("Red", "Blue", "Green", "Yellow")

@Composable
fun SimonScreen(s: AppState, onBack: () -> Unit) {
    var seq by remember { mutableStateOf(listOf(Random.nextInt(4), Random.nextInt(4))) }
    var inp by remember { mutableIntStateOf(0) }
    var lit by remember { mutableIntStateOf(-1) }
    var phase by remember { mutableIntStateOf(0) }   // 0 watch, 1 your turn, 2 oops, 3 won
    var confetti by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(seq) {
        phase = 0; inp = 0
        delay(900)
        for (c in seq) { lit = c; s.say(SIMON_NAME[c]); delay(800); lit = -1; delay(250) }
        phase = 1
    }

    fun press(c: Int) {
        if (phase != 1) return
        scope.launch { lit = c; delay(220); lit = -1 }
        s.say(SIMON_NAME[c])
        if (c == seq[inp]) {
            inp++
            if (inp == seq.size) {
                reward(s, "Memory", "Simon ${seq.size}", true)
                confetti++
                if (seq.size >= 10) { phase = 3; s.say("You are a memory champion!") } else seq = seq + Random.nextInt(4)
            }
        } else {
            phase = 2
            reward(s, "Memory", "Simon ${seq.size}", false)
            s.say("Oops! Let's try again.")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar("Simon Says", onBack, "Round ${seq.size}")
            Text(
                when (phase) { 0 -> "👀 Watch and listen…"; 1 -> "👆 Your turn!"; 2 -> "Oops! Try again"; else -> "🏆 You win!" },
                color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)
            )
            (0..1).forEach { r ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    (0..1).forEach { c ->
                        val i = r * 2 + c
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(28.dp))
                                .background(SIMON_COL[i].copy(alpha = if (lit == i) 1f else 0.5f)).clickable { press(i) },
                            contentAlignment = Alignment.Center
                        ) { Text(SIMON_NAME[i], color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
            if (phase >= 2) GradientButton("Play again", Modifier.fillMaxWidth().padding(top = 8.dp)) { seq = listOf(Random.nextInt(4), Random.nextInt(4)) }
        }
        Confetti(confetti)
    }
}

// =============================================================== Whack-a-Buddy (learn the characters' names)
@Composable
fun WhackScreen(s: AppState, onBack: () -> Unit) {
    val goal = 10
    var hits by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(BUDDIES.random()) }
    var hole by remember { mutableIntStateOf(-1) }
    var who by remember { mutableStateOf(BUDDIES[0]) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var game by remember { mutableIntStateOf(0) }

    LaunchedEffect(game) {
        delay(600)
        while (!done) {
            who = if (Random.nextInt(100) < 55) target else BUDDIES.random()
            hole = Random.nextInt(9)
            delay(1500)
            hole = -1
            delay(350)
        }
    }
    LaunchedEffect(target, game) { s.say("Tap ${target.name}!") }

    fun tap(i: Int) {
        if (hole != i || done) return
        hole = -1
        if (who.id == target.id) {
            hits++; confetti++
            reward(s, "Names", "Name ${who.name}", true)
            s.say("${Content.cheer()} That is ${who.name}!")
            if (hits >= goal) done = true else target = BUDDIES.random()
        } else {
            reward(s, "Names", "Name ${target.name}", false)
            s.say("That is ${who.name}. Find ${target.name}!")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🔨", "Great reflexes!", "+$goal ⭐", { hits = 0; done = false; target = BUDDIES.random(); game++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar("Whack-a-Buddy", onBack, "⭐ $hits/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say("Tap ${target.name}!") }) {
                Text("🔊 Tap ${target.name}!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).fillMaxWidth()) {
                (0 until 9).toList().chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { i ->
                            Box(Modifier.weight(1f).fillMaxHeight().padding(6.dp).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter)) { drawOval(Color(0xFF2B1B3A), Offset.Zero, size) }
                                if (hole == i) BuddyAvatar(who, 78.dp, Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Odd One Out
private val ODD = listOf(
    "fruits" to listOf("🍎", "🍌", "🍇", "🍉", "🍊", "🍓", "🍒", "🍑", "🍍", "🥭"),
    "animals" to listOf("🐱", "🐶", "🐰", "🦁", "🐘", "🐵", "🐸", "🐼", "🐯", "🐮"),
    "vehicles" to listOf("🚗", "🚌", "🚲", "✈️", "🚂", "🚢", "🚁", "🚚"),
    "foods" to listOf("🍕", "🍔", "🍟", "🍪", "🍩", "🎂"),
    "balls" to listOf("⚽", "🏀", "🎾", "🏈"),
    "clothes" to listOf("👕", "👖", "🧦", "🧢", "👗", "👟")
)
private class OddRound(val plural: String, val options: List<String>, val answer: String)

private fun makeOdd(): OddRound {
    val cats = ODD.shuffled()
    val a = cats[0]; val b = cats[1]
    val odd = b.second.random()
    return OddRound(a.first, (a.second.shuffled().take(3) + odd).shuffled(), odd)
}

@Composable
fun OddScreen(s: AppState, onBack: () -> Unit) {
    val goal = 8
    var score by remember { mutableIntStateOf(0) }
    var cur by remember { mutableStateOf(makeOdd()) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick, done) { if (!done) s.say("Which one is different?") }
    LaunchedEffect(chosen) {
        if (chosen != null) {
            delay(1900)
            if (score >= goal) done = true else { cur = makeOdd(); chosen = null; tick++ }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🔍", "Sharp eyes!", "+$goal ⭐", { score = 0; done = false; cur = makeOdd(); chosen = null; tick++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("Odd One Out", onBack, "⭐ $score/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say("Which one is different?") }) {
                Text("🔍 Which one is different?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(14.dp))
            cur.options.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { o ->
                        val bg = when {
                            chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                            o == cur.answer -> Brush.linearGradient(listOf(Good, Good))
                            o == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                            else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                        }
                        Box(
                            Modifier.weight(1f).padding(6.dp).height(130.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                                .clickable {
                                    if (chosen == null) {
                                        chosen = o
                                        if (o == cur.answer) {
                                            score++; confetti++
                                            reward(s, "Odd One Out", "Odd one", true)
                                            s.say("${Content.cheer()} The others are ${cur.plural}!")
                                        } else {
                                            reward(s, "Odd One Out", "Odd one", false)
                                            s.say("Look again. Three of them are ${cur.plural}.")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(o, fontSize = 62.sp) }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== Photo Friend (child's photo becomes the face of a character)
private fun loadSquare(ctx: Context, uri: Uri, target: Int = 384): Bitmap? = try {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    var sample = 1
    while (opts.outWidth / sample > target * 2 || opts.outHeight / sample > target * 2) sample *= 2
    val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
    var bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
    val rot = ctx.contentResolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
    } ?: 0f
    if (bmp != null && rot != 0f) {
        val m = Matrix(); m.postRotate(rot)
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    bmp?.let { b ->
        val side = minOf(b.width, b.height)
        val sq = Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
        Bitmap.createScaledBitmap(sq, target, target, true)
    }
} catch (_: Throwable) { null }

/** Comic look: stronger colours, fewer colour levels and dark outlines. */
private fun cartoon(src: Bitmap): Bitmap {
    val w = src.width; val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val lum = IntArray(w * h) { i -> (((px[i] shr 16) and 255) * 3 + ((px[i] shr 8) and 255) * 6 + (px[i] and 255)) / 10 }
    val out = IntArray(w * h)
    for (y in 0 until h) for (x in 0 until w) {
        val i = y * w + x
        val c = px[i]
        var r = (c shr 16) and 255; var g = (c shr 8) and 255; var b = c and 255
        val avg = (r + g + b) / 3
        r = (avg + (r - avg) * 1.5f).toInt().coerceIn(0, 255)
        g = (avg + (g - avg) * 1.5f).toInt().coerceIn(0, 255)
        b = (avg + (b - avg) * 1.5f).toInt().coerceIn(0, 255)
        r = (r * 5 / 256) * 255 / 4; g = (g * 5 / 256) * 255 / 4; b = (b * 5 / 256) * 255 / 4
        val dx = if (x + 1 < w) kotlin.math.abs(lum[i] - lum[i + 1]) else 0
        val dy = if (y + 1 < h) kotlin.math.abs(lum[i] - lum[i + w]) else 0
        out[i] = if (dx + dy > 46) 0xFF222233.toInt() else (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.setPixels(out, 0, w, 0, 0, w, h)
    return bmp
}

@Composable
fun PhotoFriendScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf<Bitmap?>(null) }
    var cartoonOn by remember { mutableStateOf(true) }
    var selId by remember { mutableStateOf(s.buddy.id) }
    var msg by remember { mutableStateOf("") }
    val photo: ImageBitmap? = remember(raw, cartoonOn) { raw?.let { (if (cartoonOn) cartoon(it) else it).asImageBitmap() } }
    val bd = buddyById(selId)

    fun intro() {
        val nm = bd.name
        val t = "Hi ${profile.name}! I am $nm. ${nm.filter { it.isLetter() }.toList().joinToString(", ")}. $nm! Can you say $nm?"
        msg = t
        s.speaker.speakEn(t, bd.pitch)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val b = loadSquare(ctx, uri)
            if (b != null) { raw = b; intro() } else msg = "Sorry, I could not open that photo."
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar("Photo Friend", onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val spec = SPECS[selId] ?: SPECS.getValue("mimo")
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(spec.body.copy(alpha = 0.38f), Color.Transparent), center = Offset(size.width / 2f, size.height * 0.5f), radius = size.width * 0.8f),
                    radius = size.width * 0.8f, center = Offset(size.width / 2f, size.height * 0.5f)
                )
                drawCharacter(spec, Pose())
                val img = photo
                if (img != null) {
                    val sc = minOf(size.width / 100f, size.height / 130f)
                    val ox = (size.width - 100f * sc) / 2f
                    val oy = (size.height - 130f * sc) / 2f
                    val hx = 50f
                    val hy = when (spec.species) { Species.SLIME -> 68f; Species.FROG -> 54f; else -> 50f }
                    val r = (if (spec.species == Species.ROBOT) 24f else 26f) * sc
                    val cx = ox + hx * sc; val cy = oy + hy * sc
                    val circle = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
                    clipPath(circle) {
                        drawImage(img, dstOffset = IntOffset((cx - r).toInt(), (cy - r).toInt()), dstSize = IntSize((2f * r).toInt(), (2f * r).toInt()))
                    }
                    drawCircle(Color.White, r, Offset(cx, cy), style = Stroke(width = 3f * sc / 2f))
                }
            }
            if (photo == null) GlassCard(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                Text("Pick a photo and it becomes a funny character!", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            if (msg.isNotEmpty()) Text(msg, color = SunYellow, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BUDDIES.forEach { b -> Box(Modifier.clickable { selId = b.id; if (raw != null) { msg = ""; } }) { BuddyAvatar(b, 54.dp) } }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientButton("📷 Photo", Modifier.weight(1f)) { picker.launch("image/*") }
            GradientButton(if (cartoonOn) "🎨 Cartoon" else "🖼 Real", Modifier.weight(1f), Color(0xFF00897B), Teal) { cartoonOn = !cartoonOn }
            GradientButton("🔊 Hi!", Modifier.weight(1f), Color(0xFFFF6F00), Color(0xFFFFB300)) { intro() }
        }
    }
}
