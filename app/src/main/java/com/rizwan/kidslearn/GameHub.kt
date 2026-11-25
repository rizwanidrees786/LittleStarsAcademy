package com.rizwan.kidslearn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =============================================================== Content packs (pictures + names)
data class Item(val e: String, val name: String)

object Packs {
    private fun p(vararg x: Pair<String, String>) = x.map { Item(it.first, it.second) }
    private val numNames = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
        "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty")

    val all: Map<String, Pair<String, List<Item>>> = mapOf(
        "animals" to ("Animals" to p("🐱" to "Cat", "🐶" to "Dog", "🐰" to "Rabbit", "🦁" to "Lion", "🐘" to "Elephant", "🐵" to "Monkey", "🐸" to "Frog", "🐼" to "Panda",
            "🐯" to "Tiger", "🐮" to "Cow", "🐷" to "Pig", "🐔" to "Hen", "🐧" to "Penguin", "🦆" to "Duck", "🦉" to "Owl", "🐴" to "Horse", "🐑" to "Sheep", "🐢" to "Turtle", "🐟" to "Fish", "🦋" to "Butterfly")),
        "fruits" to ("Fruits" to p("🍎" to "Apple", "🍌" to "Banana", "🍇" to "Grapes", "🍉" to "Watermelon", "🍊" to "Orange", "🍓" to "Strawberry", "🍒" to "Cherry", "🍑" to "Peach", "🍍" to "Pineapple", "🥭" to "Mango", "🍋" to "Lemon", "🍐" to "Pear")),
        "vehicles" to ("Vehicles" to p("🚗" to "Car", "🚌" to "Bus", "🚲" to "Bicycle", "✈️" to "Airplane", "🚂" to "Train", "🚢" to "Ship", "🚁" to "Helicopter", "🚚" to "Truck", "🚑" to "Ambulance", "🚜" to "Tractor", "🚀" to "Rocket", "⛵" to "Boat")),
        "foods" to ("Food" to p("🍕" to "Pizza", "🍔" to "Burger", "🍟" to "Fries", "🍪" to "Cookie", "🍩" to "Donut", "🎂" to "Cake", "🍞" to "Bread", "🧀" to "Cheese", "🥚" to "Egg", "🍦" to "Ice cream")),
        "clothes" to ("Clothes" to p("👕" to "Shirt", "👖" to "Jeans", "🧦" to "Socks", "🧢" to "Cap", "👗" to "Dress", "👟" to "Shoe", "🧤" to "Gloves", "🧥" to "Coat")),
        "objects" to ("Things" to p("⚽" to "Ball", "🎈" to "Balloon", "🎁" to "Gift", "🔑" to "Key", "🔔" to "Bell", "🎩" to "Hat", "👑" to "Crown", "📖" to "Book", "✏️" to "Pencil", "🎒" to "Bag", "☂️" to "Umbrella", "⏰" to "Clock", "🧸" to "Teddy", "📱" to "Phone", "💡" to "Bulb")),
        "nature" to ("Nature" to p("☀️" to "Sun", "🌙" to "Moon", "⭐" to "Star", "☁️" to "Cloud", "🌳" to "Tree", "🌸" to "Flower", "🍃" to "Leaf", "🔥" to "Fire", "💧" to "Water", "❄️" to "Snow", "🌈" to "Rainbow", "⛰️" to "Mountain")),
        "shapes" to ("Shapes" to p("🔺" to "Triangle", "🟦" to "Square", "⚪" to "Circle", "❤️" to "Heart", "🔶" to "Diamond", "⭐" to "Star", "🟥" to "Red square", "🟢" to "Green circle")),
        "colors" to ("Colors" to p("🔴" to "Red", "🔵" to "Blue", "🟢" to "Green", "🟡" to "Yellow", "🟠" to "Orange", "🟣" to "Purple", "🟤" to "Brown", "⚫" to "Black", "⚪" to "White")),
        "faces" to ("Feelings" to p("😀" to "Happy", "😢" to "Sad", "😡" to "Angry", "😴" to "Sleepy", "😍" to "Loving", "😮" to "Surprised", "😱" to "Scared", "🤒" to "Sick")),
        "music" to ("Music" to p("🎸" to "Guitar", "🥁" to "Drum", "🎹" to "Piano", "🎺" to "Trumpet", "🎻" to "Violin", "🎤" to "Microphone")),
        "space" to ("Space" to p("🚀" to "Rocket", "🛸" to "UFO", "🌍" to "Earth", "🌙" to "Moon", "⭐" to "Star", "☄️" to "Comet", "☀️" to "Sun", "👽" to "Alien")),
        "body" to ("Body" to p("👁️" to "Eye", "👂" to "Ear", "👃" to "Nose", "👄" to "Mouth", "✋" to "Hand", "🦶" to "Foot", "🦷" to "Tooth", "👅" to "Tongue")),
        "helpers" to ("Helpers" to p("👨‍⚕️" to "Doctor", "👩‍🏫" to "Teacher", "👮" to "Policeman", "👨‍🚒" to "Firefighter", "👨‍🌾" to "Farmer", "👨‍🍳" to "Chef", "👨‍✈️" to "Pilot", "👨‍🔧" to "Mechanic")),
        "duas" to ("Duas" to p("🍽️" to "Before Eating", "😋" to "After Eating", "🛏️" to "Sleeping", "🌅" to "Waking Up", "🚽" to "Toilet", "🏠" to "Home", "🕌" to "Masjid", "👕" to "New Clothes", "🤧" to "Sneezing", "🚗" to "Travel", "🌧️" to "Rain", "👨‍👩‍👧" to "Parents")),
        "letters" to ("Letters" to ('A'..'Z').map { Item(it.toString(), it.toString()) }),
        "numbers" to ("Numbers" to (1..20).map { Item(it.toString(), numNames[it]) }),
    )

    fun items(id: String): List<Item> = all[id]?.second ?: emptyList()
    fun title(id: String): String = all[id]?.first ?: id
}

// =============================================================== Catalog of games
enum class GK { FLIP, FLIPW, SIMON, MISSING, ORDER, SEEN, FLASH, GRID, PICKP, PICKW, BALLOON, WHACK, SORT, SK_CLASSIC, SK_RACE, SK_STREAK, SK_TF, CLASSIC, PATTERN, SUDOKU, MATCH, MAKE, SUBIT, STROOP }

data class GameDef(
    val id: String, val title: String, val emoji: String, val cat: String, val sub: String, val kind: GK,
    val pack: String = "", val pack2: String = "", val skill: String = "", val screen: Screen? = null
)

private fun skillEmoji(sk: String): String = when (sk) {
    "Letters" -> "🔤"; "Phonics" -> "🗣️"; "Spelling" -> "✍️"; "Counting" -> "🔢"; "Numbers" -> "🔟"; "Addition" -> "➕"
    "Subtraction" -> "➖"; "Multiplication" -> "✖️"; "Comparing" -> "⚖️"; "Place Value" -> "🏠"; "Patterns" -> "🧩"; "Shapes" -> "🔷"
    "Colors" -> "🎨"; "Animals" -> "🐾"; "Body & Senses" -> "👁️"; "Days & Months" -> "📅"; "Opposites" -> "↔️"; "Synonyms" -> "🟰"
    "Grammar" -> "📝"; "Words" -> "📖"; "Sight Words" -> "👀"; "Rhymes" -> "🎵"; "Urdu → English" -> "🇵🇰"; "Urdu Alphabet" -> "ا"
    "Islamiat" -> "🕌"; "Manners" -> "🤝"; "Science" -> "🔬"; "Pakistan & GK" -> "🌍"; "Helpers" -> "👷"; "Transport" -> "🚌"
    "Family" -> "👪"; "Time" -> "⏰"; "Word Problems" -> "📚"; "Fruits & Food" -> "🍎"; "Nature" -> "🌳"; "Odd One Out" -> "🔍"
    else -> "🎯"
}

object Catalog {
    private fun mem(kind: GK, fmt: String, emoji: String, sub: String, packs: List<String>) =
        packs.map { pk -> GameDef("${kind.name}_$pk", fmt.replace("%", Packs.title(pk)), emoji, "🧠 Memory", sub, kind, pk) }

    fun build(level: Int, skills: List<String>): List<GameDef> {
        val out = ArrayList<GameDef>()
        out += mem(GK.FLIP, "% Match", "🃏", "Flip cards, find the pairs", listOf("animals", "fruits", "vehicles", "foods", "nature", "objects", "clothes", "duas"))
        out += mem(GK.FLIPW, "% Picture-Word Match", "🔤", "Match each picture with its word", listOf("animals", "fruits", "vehicles", "foods", "objects", "body", "helpers", "duas"))
        out += mem(GK.SIMON, "% Simon", "🎵", "Watch, listen and repeat", listOf("colors", "animals", "fruits", "vehicles", "shapes", "music"))
        out += mem(GK.MISSING, "What is Missing? %", "❓", "Which one disappeared?", listOf("animals", "fruits", "vehicles", "foods", "objects", "nature", "clothes", "space", "duas"))
        out += mem(GK.ORDER, "% Order", "🔢", "Tap in the same order", listOf("animals", "fruits", "vehicles", "shapes", "colors", "numbers"))
        out += mem(GK.SEEN, "Did You See It? %", "👀", "Yes or no?", listOf("animals", "fruits", "foods", "objects", "duas"))
        out += mem(GK.FLASH, "Quick Flash: %", "⚡", "Remember the row", listOf("numbers", "letters", "colors", "shapes"))
        out += mem(GK.GRID, "Where Was It? %", "🧩", "Remember the places", listOf("animals", "fruits", "vehicles", "faces"))
        out += mem(GK.PICKP, "Remember the Picture: %", "🖼️", "Which one did you see?", listOf("animals", "fruits", "vehicles", "space"))
        out += mem(GK.PICKW, "Remember the Word: %", "📝", "Which word did you see?", listOf("animals", "fruits", "body", "helpers", "duas"))
        for (pk in listOf("letters", "numbers", "animals", "fruits", "vehicles", "shapes", "colors"))
            out.add(GameDef("BALLOON_$pk", "Balloon Pop: ${Packs.title(pk)}", "🎈", "🎯 Pop & Tap", "Pop the right balloon", GK.BALLOON, pk))
        for (pk in listOf("letters", "numbers", "animals", "fruits", "shapes", "colors"))
            out.add(GameDef("WHACK_$pk", "Whack: ${Packs.title(pk)}", "🔨", "🎯 Pop & Tap", "Tap the one you hear", GK.WHACK, pk))
        for ((a, b) in listOf("animals" to "fruits", "vehicles" to "animals", "foods" to "clothes", "nature" to "objects", "fruits" to "vehicles", "animals" to "nature", "foods" to "objects"))
            out.add(GameDef("SORT_${a}_$b", "Sort: ${Packs.title(a)} or ${Packs.title(b)}?", "🗂️", "🎯 Pop & Tap", "Put it in the right box", GK.SORT, a, b))
        val bb = "🧩 Brain Boost"
        out.add(GameDef("PATTERN_num", "Number Patterns", "🔢", bb, "What number comes next?", GK.PATTERN, "num"))
        out.add(GameDef("PATTERN_shape", "Picture Patterns", "🔴", bb, "What comes next?", GK.PATTERN, "shape"))
        out.add(GameDef("SUDOKU_animals", "Picture Sudoku: Animals", "🐱", bb, "Each picture once in every row and column", GK.SUDOKU, "animals"))
        out.add(GameDef("SUDOKU_fruits", "Picture Sudoku: Fruits", "🍎", bb, "Each picture once in every row and column", GK.SUDOKU, "fruits"))
        for (pk in listOf("animals", "fruits", "vehicles", "letters", "numbers", "colors", "body", "duas"))
            out.add(GameDef("MATCH_$pk", "Match Up: ${Packs.title(pk)}", "🔗", bb, "Join each picture with its name", GK.MATCH, pk))
        for (t in listOf(5, 10, 20)) out.add(GameDef("MAKE_$t", "Make $t", "➕", bb, "Find two numbers that make $t", GK.MAKE, t.toString()))
        out.add(GameDef("SUBIT_dots", "Quick Count: Dots", "⚡", bb, "Flash and count!", GK.SUBIT, "dots"))
        out.add(GameDef("SUBIT_animals", "Quick Count: Animals", "🐾", bb, "Flash and count!", GK.SUBIT, "animals"))
        out.add(GameDef("STROOP_colors", "Color Magic", "🎨", bb, "Tap the color of the letters", GK.STROOP, "colors"))
        for (sk in skills) {
            val em = skillEmoji(sk)
            out.add(GameDef("SKC_$sk", sk, em, "📚 Skills", "Classic: 8 questions", GK.SK_CLASSIC, skill = sk))
            out.add(GameDef("SKR_$sk", "$sk Race", "⏱️", "📚 Skills", "60 seconds, answer fast!", GK.SK_RACE, skill = sk))
            out.add(GameDef("SKS_$sk", "$sk Streak", "🔥", "📚 Skills", "3 lives, how far can you go?", GK.SK_STREAK, skill = sk))
            out.add(GameDef("SKT_$sk", "$sk True or False", "✅", "📚 Skills", "Is the answer right?", GK.SK_TF, skill = sk))
        }
        out.add(GameDef("C_balloon", "Balloon Math", "🎈", "⭐ Classics", "Letters, numbers and sums", GK.CLASSIC, screen = Screen.Balloons))
        out.add(GameDef("C_memory", "Memory Match", "🧠", "⭐ Classics", "Find the pairs", GK.CLASSIC, screen = Screen.Memory))
        out.add(GameDef("C_paint", "Magic Paint", "🎨", "⭐ Classics", "Draw with your finger", GK.CLASSIC, screen = Screen.Paint))
        out.add(GameDef("C_safari", "Sound Safari", "🐮", "⭐ Classics", "Who says moo?", GK.CLASSIC, screen = Screen.Safari))
        out.add(GameDef("C_stickers", "Sticker Book", "⭐", "⭐ Classics", "Collect stickers", GK.CLASSIC, screen = Screen.Stickers))
        out.add(GameDef("C_spell", "Spelling Bee", "🐝", "⭐ Classics", "Tap letters in order", GK.CLASSIC, screen = Screen.Spell))
        out.add(GameDef("C_simon", "Simon Says", "🔴", "⭐ Classics", "Remember the colors", GK.CLASSIC, screen = Screen.Simon))
        out.add(GameDef("C_whack", "Whack-a-Buddy", "🔨", "⭐ Classics", "Tap the named friend", GK.CLASSIC, screen = Screen.Whack))
        out.add(GameDef("C_odd", "Odd One Out", "🔍", "⭐ Classics", "Which is different?", GK.CLASSIC, screen = Screen.Odd))
        out.add(GameDef("C_photo", "Photo Friend", "📸", "⭐ Classics", "Your face on a hero", GK.CLASSIC, screen = Screen.PhotoFriend))
        out.add(GameDef("C_speak", "Speak & Practice", "🗣️", "⭐ Classics", "Say it, win stars", GK.CLASSIC, screen = Screen.Speak))
        return out
    }
}

@Composable
fun AllGamesScreen(s: AppState, go: (Screen) -> Unit, onBack: () -> Unit) {
    val profile = s.profile ?: return
    var skills by remember { mutableStateOf(Bank.skills(profile.level)) }
    LaunchedEffect(Unit) {
        var t = 0
        while (skills.isEmpty() && t < 20) { delay(300); skills = Bank.skills(profile.level); t++ }
    }
    val games = remember(skills) { Catalog.build(profile.level, skills) }
    val cats = remember(games) { games.map { it.cat }.distinct() }
    var cat by remember { mutableStateOf("🧠 Memory") }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TopBar("All Games", onBack, "${games.size} games")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c -> Chip("$c (${games.count { it.cat == c }})", cat == c, Modifier.width(170.dp)) { cat = c } }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(games.filter { it.cat == cat }) { g ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { go(g.screen ?: Screen.Game(g.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(g.emoji, fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(g.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(g.sub, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameHost(s: AppState, id: String, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val def = remember(id) { Catalog.build(profile.level, Bank.skills(profile.level)).firstOrNull { it.id == id } }
    if (def == null) { LaunchedEffect(Unit) { onBack() }; return }
    when (def.kind) {
        GK.FLIP -> FlipGame(s, def, false, onBack)
        GK.FLIPW -> FlipGame(s, def, true, onBack)
        GK.SIMON -> SimonGame(s, def, onBack)
        GK.MISSING -> MissingGame(s, def, onBack)
        GK.ORDER -> OrderGame(s, def, onBack)
        GK.SEEN -> SeenGame(s, def, onBack)
        GK.FLASH -> FlashGame(s, def, onBack)
        GK.GRID -> GridGame(s, def, onBack)
        GK.PICKP -> PickGame(s, def, false, onBack)
        GK.PICKW -> PickGame(s, def, true, onBack)
        GK.BALLOON -> BalloonGame(s, def, onBack)
        GK.WHACK -> WhackGame2(s, def, onBack)
        GK.SORT -> SortGame(s, def, onBack)
        GK.SK_CLASSIC, GK.SK_RACE, GK.SK_STREAK, GK.SK_TF -> SkillGame(s, def, onBack)
        GK.PATTERN -> PatternGame(s, def, onBack)
        GK.SUDOKU -> SudokuGame(s, def, onBack)
        GK.MATCH -> MatchGame(s, def, onBack)
        GK.MAKE -> MakeGame(s, def, onBack)
        GK.SUBIT -> SubitGame(s, def, onBack)
        GK.STROOP -> StroopGame(s, def, onBack)
        GK.CLASSIC -> LaunchedEffect(Unit) { onBack() }
    }
}

private fun poolOf(d: GameDef, level: Int): List<Item> {
    val l = Packs.items(d.pack)
    return if (d.pack == "numbers" && level == 1) l.take(10) else l
}

@Composable
fun ChoiceGrid(opts: List<Item>, chosen: String?, answer: String, asText: Boolean, onPick: (Item) -> Unit) {
    opts.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { o ->
                val bg = when {
                    chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                    o.name == answer -> Brush.linearGradient(listOf(Good, Good))
                    o.name == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                    else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                }
                Box(
                    Modifier.weight(1f).padding(6.dp).height(110.dp).clip(RoundedCornerShape(24.dp)).background(bg)
                        .clickable { if (chosen == null) onPick(o) },
                    contentAlignment = Alignment.Center
                ) {
                    if (asText) Text(o.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    else Text(o.e, fontSize = 52.sp)
                }
            }
        }
    }
}

// =============================================================== 1-2. Flip match (pictures, or picture + word)
private data class GCard(val key: String, val face: String, val word: Boolean, val up: Boolean = false, val done: Boolean = false)

@Composable
private fun GFlipCard(c: GCard, modifier: Modifier, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (c.up || c.done) 180f else 0f, tween(350), label = "gf")
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
            ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (front) Box(Modifier.graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
            Text(c.face, color = Color.White, fontSize = if (c.word) 15.sp else 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        } else Text("❓", fontSize = 30.sp)
    }
}

@Composable
fun FlipGame(s: AppState, d: GameDef, words: Boolean, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val pairs = if (profile.level >= 3) 8 else 6
    val cols = if (pairs == 8) 4 else 3
    var game by remember { mutableIntStateOf(0) }
    val cards = remember(game) {
        mutableStateListOf<GCard>().also { l ->
            Packs.items(d.pack).shuffled().take(pairs).forEach { item ->
                l.add(GCard(item.name, item.e, false))
                l.add(if (words) GCard(item.name, item.name, true) else GCard(item.name, item.e, false))
            }
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
            scope.launch { delay(850); cards[a] = cards[a].copy(up = false); cards[i] = cards[i].copy(up = false); busy = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (found == pairs) DoneView("🃏", "All pairs found!", "$moves moves  +$pairs ⭐", { game++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "Moves $moves")
            Text("Flip two cards and find the match!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            cards.indices.toList().chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { i -> GFlipCard(cards[i], Modifier.weight(1f).fillMaxHeight().padding(4.dp)) { tap(i) } }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 3. Simon (pictures)
private val PAD_COLS = listOf(Color(0xFFFF5252), Color(0xFF40C4FF), Color(0xFF00E676), Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFFFF9800))

@Composable
fun SimonGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val npads = if (profile.level >= 3) 6 else 4
    var game by remember { mutableIntStateOf(0) }
    val pads = remember(game) { Packs.items(d.pack).shuffled().take(npads) }
    var seq by remember(game) { mutableStateOf(listOf(Random.nextInt(npads), Random.nextInt(npads))) }
    var inp by remember(game) { mutableIntStateOf(0) }
    var lit by remember { mutableIntStateOf(-1) }
    var phase by remember(game) { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(seq) {
        phase = 0; inp = 0
        delay(900)
        for (c in seq) { lit = c; s.say(pads[c].name); delay(850); lit = -1; delay(250) }
        phase = 1
    }
    fun press(c: Int) {
        if (phase != 1) return
        scope.launch { lit = c; delay(220); lit = -1 }
        s.say(pads[c].name)
        if (c == seq[inp]) {
            inp++
            if (inp == seq.size) {
                reward(s, "Memory", "Simon ${seq.size}", true); confetti++
                if (seq.size >= 10) { phase = 3; s.say("You are a memory champion!") } else seq = seq + Random.nextInt(npads)
            }
        } else { phase = 2; reward(s, "Memory", "Simon ${seq.size}", false); s.say("Oops! Let's try again.") }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "Round ${seq.size}")
            Text(when (phase) { 0 -> "👀 Watch and listen…"; 1 -> "👆 Your turn!"; 2 -> "Oops! Try again"; else -> "🏆 You win!" },
                color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            (0 until npads).toList().chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { i ->
                        Column(
                            Modifier.weight(1f).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(28.dp))
                                .background(PAD_COLS[i].copy(alpha = if (lit == i) 1f else 0.5f)).clickable { press(i) },
                            Arrangement.Center, Alignment.CenterHorizontally
                        ) {
                            Text(pads[i].e, fontSize = 44.sp)
                            Text(pads[i].name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (phase >= 2) GradientButton("Play again", Modifier.fillMaxWidth().padding(top = 8.dp)) { game++ }
        }
        Confetti(confetti)
    }
}

// =============================================================== 4. What is missing?
@Composable
fun MissingGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val k = when (profile.level) { 1 -> 3; 2 -> 4; else -> 5 }
    val goal = 6
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    val setup = remember(round) {
        val all = Packs.items(d.pack).shuffled()
        val shown = all.take(k)
        val miss = Random.nextInt(k)
        Triple(shown, miss, (all.drop(k).take(3) + shown[miss]).shuffled())
    }
    var phase by remember(round) { mutableIntStateOf(0) }
    LaunchedEffect(round) {
        chosen = null; phase = 0
        s.say("Remember the pictures!")
        delay(1800L + k * 1000L)
        phase = 1
        s.say("What is missing?")
    }
    LaunchedEffect(chosen) {
        if (chosen != null) { delay(2000); if (round + 1 >= goal) done = true else round++ }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🧠", "Great memory!", "$score of $goal right  +$score ⭐", { round = 0; score = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            Text(if (phase == 0) "👀 Look carefully…" else "❓ What is missing?", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                setup.first.forEachIndexed { i, it ->
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(if (phase == 1 && i == setup.second) "❓" else it.e, fontSize = 34.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            if (phase == 1) ChoiceGrid(setup.third, chosen, setup.first[setup.second].name, false) { o ->
                chosen = o.name
                if (o.name == setup.first[setup.second].name) {
                    score++; confetti++; reward(s, "Memory", "Missing", true); s.say("${Content.cheer()} It was the ${o.name}!")
                } else { reward(s, "Memory", "Missing", false); s.say("It was the ${setup.first[setup.second].name}.") }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 5. Order
@Composable
fun OrderGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val len = profile.level + 2
    val goal = 5
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val pool = remember { poolOf(d, profile.level) }
    val setup = remember(round) {
        val seq = pool.shuffled().take(len)
        var mix = seq.shuffled()
        if (mix == seq) mix = seq.reversed()
        Pair(seq, mix)
    }
    var phase by remember(round) { mutableIntStateOf(0) }
    var pos by remember(round) { mutableIntStateOf(0) }
    val used = remember(round) { mutableStateListOf<Boolean>().also { l -> repeat(len) { l.add(false) } } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(round) {
        phase = 0
        s.say("Remember the order!")
        delay(1500L + len * 1000L)
        phase = 1
        s.say("Now tap them in the same order!")
    }
    fun tap(i: Int) {
        if (phase != 1 || used[i] || pos >= len) return
        val it0 = setup.second[i]
        if (it0.name == setup.first[pos].name) {
            used[i] = true; pos++; s.say(it0.name)
            if (pos == len) {
                confetti++; reward(s, "Memory", "Order ${d.pack}", true); s.say(Content.cheer())
                scope.launch { delay(1800); if (round + 1 >= goal) done = true else round++ }
            }
        } else { reward(s, "Memory", "Order ${d.pack}", false); s.say("Try another one.") }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🔢", "Perfect order!", "+$goal ⭐", { round = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "Round ${round + 1}/$goal")
            if (phase == 0) {
                Text("👀 Remember this order", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    setup.first.forEachIndexed { i, it ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${i + 1}", color = Teal, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text(it.e, fontSize = 32.sp) }
                        }
                    }
                }
            } else {
                Text("👆 Tap them in the same order", color = SunYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    setup.first.forEachIndexed { i, it ->
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(if (i < pos) Good else Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            if (i < pos) Text(it.e, fontSize = 30.sp)
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    setup.second.forEachIndexed { i, it ->
                        Box(
                            Modifier.size(58.dp).alpha(if (used[i]) 0.25f else 1f).clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Purple, Pink))).clickable { tap(i) },
                            contentAlignment = Alignment.Center
                        ) { Text(it.e, fontSize = 32.sp) }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 6. Did you see it?
@Composable
fun SeenGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val goal = 3
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val setup = remember(round) {
        val all = Packs.items(d.pack).shuffled()
        val seen = all.take(4)
        val probes = (seen.take(2).map { it to true } + all.drop(4).take(2).map { it to false }).shuffled()
        Pair(seen, probes)
    }
    var phase by remember(round) { mutableIntStateOf(0) }
    var pi by remember(round) { mutableIntStateOf(0) }
    var fb by remember(round) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(round) {
        phase = 0; pi = 0; fb = ""
        s.say("Look at the pictures!")
        delay(4000)
        phase = 1
        s.say("Was this one there?")
    }
    fun answer(yes: Boolean) {
        if (phase != 1 || fb.isNotEmpty() || pi >= setup.second.size) return
        val ok = yes == setup.second[pi].second
        total++
        reward(s, "Memory", "Seen it", ok)
        if (ok) { score++; confetti++; fb = "✅" } else fb = "❌"
        s.say(if (ok) Content.cheer() else if (setup.second[pi].second) "Yes, it was there." else "No, it was not there.")
        scope.launch {
            delay(1300)
            fb = ""
            pi++
            if (pi >= setup.second.size) { if (round + 1 >= goal) done = true else round++ } else s.say("Was this one there?")
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("👀", "Sharp memory!", "$score of $total right  +$score ⭐", { round = 0; score = 0; total = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score")
            if (phase == 0) {
                Text("👀 Remember these", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    setup.first.forEach { Box(Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text(it.e, fontSize = 40.sp) } }
                }
            } else if (pi < setup.second.size) {
                Text("Was this one there?", color = SunYellow, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(setup.second[pi].first.e, fontSize = 110.sp)
                Text(fb, fontSize = 40.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    GradientButton("👍 Yes", Modifier.weight(1f).padding(6.dp), Good, Color(0xFF00E676)) { answer(true) }
                    GradientButton("👎 No", Modifier.weight(1f).padding(6.dp), Bad, Color(0xFFFF8A80)) { answer(false) }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 7. Quick flash (remember a row, then enter it)
@Composable
fun FlashGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    var game by remember { mutableIntStateOf(0) }
    val alphabet = remember(game) { poolOf(d, profile.level).shuffled().take(if (profile.level >= 3) 8 else 6) }
    var n by remember(game) { mutableIntStateOf(3) }
    var seq by remember(game, n) { mutableStateOf(List(n) { alphabet.random() }) }
    var input by remember(game, n) { mutableStateOf(listOf<Item>()) }
    var phase by remember(game, n) { mutableIntStateOf(0) }   // 0 show, 1 enter, 2 wrong, 3 won
    var confetti by remember { mutableIntStateOf(0) }

    LaunchedEffect(game, n) {
        phase = 0
        s.say("Remember!")
        delay(1000L + n * 750L)
        phase = 1
        s.say("Now you!")
    }
    fun tap(it0: Item) {
        if (phase != 1) return
        s.say(it0.name)
        val idx = input.size
        if (it0.name != seq[idx].name) { phase = 2; reward(s, "Memory", "Flash $n", false); s.say("Oops!"); return }
        input = input + it0
        if (input.size == n) {
            reward(s, "Memory", "Flash $n", true); confetti++
            if (n >= 8) { phase = 3; s.say("Amazing memory!") } else n++
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "Length $n")
            Text(when (phase) { 0 -> "👀 Remember the row"; 1 -> "👆 Tap them in order"; 2 -> "Oops! It was:"; else -> "🏆 You win!" }, color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (phase == 0 || phase == 2) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                seq.forEach { Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text(it.e, fontSize = 24.sp) } }
            }
            if (phase == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    seq.indices.forEach { i -> Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (i < input.size) Good else Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { if (i < input.size) Text(input[i].e, fontSize = 24.sp) } }
                }
                Spacer(Modifier.height(24.dp))
                alphabet.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { it0 ->
                            Box(Modifier.weight(1f).padding(6.dp).height(80.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Purple, Pink))).clickable { tap(it0) }, contentAlignment = Alignment.Center) { Text(it0.e, fontSize = 34.sp) }
                        }
                    }
                }
            }
            if (phase >= 2) GradientButton("Play again", Modifier.fillMaxWidth().padding(top = 16.dp)) { game++ }
        }
        Confetti(confetti)
    }
}

// =============================================================== 8. Where was it? (positions)
@Composable
fun GridGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val cols = if (profile.level >= 3) 4 else 3
    val rows = if (profile.level == 1) 3 else 4
    val cells = cols * rows
    val goal = 5
    var round by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var done by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val k = minOf(2 + round + (if (profile.level >= 2) 1 else 0), 7)
    val faces = remember(round) { List(cells) { Packs.items(d.pack).random() } }
    val target = remember(round) { (0 until cells).shuffled().take(k).toSet() }
    val found = remember(round) { mutableStateListOf<Int>() }
    var phase by remember(round) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(round) {
        phase = 0
        s.say("Remember the glowing places!")
        delay(2200L + k * 300L)
        phase = 1
        s.say("Now tap where they were!")
    }
    fun tap(i: Int) {
        if (phase != 1 || i in found || done) return
        if (i in target) {
            found.add(i); s.say(faces[i].name)
            if (found.size == k) {
                confetti++; reward(s, "Memory", "Places", true); s.say(Content.cheer())
                scope.launch { delay(1500); if (round + 1 >= goal) { won = true; done = true } else round++ }
            }
        } else {
            lives--; reward(s, "Memory", "Places", false); s.say("Oops!")
            if (lives <= 0) done = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView(if (won) "🏆" else "🧩", if (won) "Great memory!" else "Good try!", "Round ${round + 1}", { round = 0; lives = 3; done = false; won = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "♥".repeat(lives))
            Text(if (phase == 0) "👀 Remember the glowing places" else "👆 Tap where they were ($k)", color = SunYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            (0 until cells).toList().chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { i ->
                        val shown = phase == 0 || i in found
                        val bg = when { phase == 0 && i in target -> Color(0xAAFFD54F); i in found -> Good; else -> Color.White.copy(alpha = 0.14f) }
                        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                            Text(if (shown) faces[i].e else "❓", fontSize = 30.sp)
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 9-10. Remember the picture / the word
@Composable
fun PickGame(s: AppState, d: GameDef, asWord: Boolean, onBack: () -> Unit) {
    val goal = 8
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    val setup = remember(round) {
        val all = Packs.items(d.pack).shuffled()
        Pair(all[0], (all.drop(1).take(3) + all[0]).shuffled())
    }
    var phase by remember(round) { mutableIntStateOf(0) }
    LaunchedEffect(round) {
        chosen = null; phase = 0
        s.say(if (asWord) "Remember the word!" else "Remember the picture!")
        delay(2200)
        phase = 1
        s.say(if (asWord) "Which word did you see?" else "Which picture did you see?")
    }
    LaunchedEffect(chosen) { if (chosen != null) { delay(1800); if (round + 1 >= goal) done = true else round++ } }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🖼️", "Great memory!", "$score of $goal right  +$score ⭐", { round = 0; score = 0; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            if (phase == 0) {
                Text("👀 Remember this", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(30.dp))
                if (asWord) Text(setup.first.name, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold) else Text(setup.first.e, fontSize = 120.sp)
            } else {
                Text(if (asWord) "Which word did you see?" else "Which one did you see?", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                ChoiceGrid(setup.second, chosen, setup.first.name, asWord) { o ->
                    chosen = o.name
                    if (o.name == setup.first.name) { score++; confetti++; reward(s, "Memory", "Remember ${d.pack}", true); s.say(Content.cheer()) }
                    else { reward(s, "Memory", "Remember ${d.pack}", false); s.say("It was ${setup.first.name}.") }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 11. Balloon pop with any picture pack
private data class GBalloon(val id: Int, val item: Item, val x: Float, val y: Float, val speed: Float, val color: Color)

@Composable
fun BalloonGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 8
    val pool = remember { poolOf(d, profile.level) }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(pool.random()) }
    val balloons = remember { mutableStateListOf<GBalloon>() }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val colors = listOf(Pink, Teal, SunYellow, Sky, Purple, Color(0xFFFF7043))

    LaunchedEffect(round) {
        val all = (pool.filter { it.name != target.name }.shuffled().take(4) + target).shuffled()
        balloons.clear()
        all.forEachIndexed { i, it0 -> balloons.add(GBalloon(i, it0, i * 0.2f + Random.nextFloat() * 0.03f, 0.9f + Random.nextFloat() * 0.6f, 0.10f + Random.nextFloat() * 0.10f, colors[i % colors.size])) }
        s.say("Pop the ${target.name}!")
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
    fun pop(b: GBalloon) {
        if (done) return
        if (b.item.name == target.name) {
            reward(s, "Reflex", "Pop ${target.name}", true); score++; confetti++; s.say(Content.cheer())
            if (score >= goal) done = true else { target = pool.random(); round++ }
        } else { reward(s, "Reflex", "Pop ${target.name}", false); s.say("That is ${b.item.name}. Find ${target.name}!") }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🎈", "Great job!", "+$goal ⭐", { score = 0; done = false; target = pool.random(); round++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "⭐ $score/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say("Pop the ${target.name}!") }) {
                Text("🔊 Pop the ${target.name}!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
            val bw = with(LocalDensity.current) { 84.dp.roundToPx() }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val w = constraints.maxWidth
                val h = constraints.maxHeight
                balloons.forEach { b ->
                    key(b.id) {
                        Box(
                            Modifier.offset { IntOffset((b.x * (w - bw)).toInt(), (b.y * h).toInt()) }.size(84.dp, 104.dp).clip(RoundedCornerShape(50))
                                .background(Brush.verticalGradient(listOf(b.color, b.color.copy(alpha = 0.65f)))).clickable { pop(b) },
                            contentAlignment = Alignment.Center
                        ) { Text(b.item.e, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 12. Whack with any picture pack
@Composable
fun WhackGame2(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val goal = 10
    val pool = remember { poolOf(d, profile.level) }
    var hits by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(pool.random()) }
    var hole by remember { mutableIntStateOf(-1) }
    var who by remember { mutableStateOf(pool.first()) }
    var confetti by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var game by remember { mutableIntStateOf(0) }

    LaunchedEffect(game) {
        delay(600)
        while (!done) {
            who = if (Random.nextInt(100) < 50) target else pool.random()
            hole = Random.nextInt(9)
            delay(1500)
            hole = -1
            delay(350)
        }
    }
    LaunchedEffect(target, game) { s.say("Tap the ${target.name}!") }
    fun tap(i: Int) {
        if (hole != i || done) return
        hole = -1
        if (who.name == target.name) {
            hits++; confetti++; reward(s, "Reflex", "Whack ${who.name}", true); s.say("${Content.cheer()} ${who.name}!")
            if (hits >= goal) done = true else target = pool.random()
        } else { reward(s, "Reflex", "Whack ${target.name}", false); s.say("That is ${who.name}. Find ${target.name}!") }
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🔨", "Great reflexes!", "+$goal ⭐", { hits = 0; done = false; target = pool.random(); game++ }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp)) {
            TopBar(d.title, onBack, "⭐ $hits/$goal")
            GlassCard(Modifier.fillMaxWidth().clickable { s.say("Tap the ${target.name}!") }) {
                Text("🔊 Tap the ${target.name}!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).fillMaxWidth()) {
                (0 until 9).toList().chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { i ->
                            Box(Modifier.weight(1f).fillMaxHeight().padding(6.dp).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter)) { drawOval(Color(0xFF2B1B3A), Offset.Zero, size) }
                                if (hole == i) Text(who.e, fontSize = 52.sp, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 13. Sort into two boxes
@Composable
fun SortGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val a = Packs.items(d.pack)
    val b = Packs.items(d.pack2)
    val goal = 10
    fun next(): Pair<Item, Boolean> = if (Random.nextBoolean()) a.random() to true else b.random() to false
    var n by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var cur by remember { mutableStateOf(next()) }
    var chosen by remember { mutableStateOf<Boolean?>(null) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(n) { s.say("Where does the ${cur.first.name} go?") }
    LaunchedEffect(chosen) {
        if (chosen != null) { delay(1300); if (n + 1 >= goal) done = true else { cur = next(); chosen = null; n++ } }
    }
    fun pick(isA: Boolean) {
        if (chosen != null) return
        chosen = isA
        val ok = isA == cur.second
        reward(s, "Sorting", "Sort ${cur.first.name}", ok)
        if (ok) { score++; confetti++; s.say(Content.cheer()) } else s.say("A ${cur.first.name} goes with ${Packs.title(if (cur.second) d.pack else d.pack2)}.")
    }

    Box(Modifier.fillMaxSize()) {
        if (done) DoneView("🗂️", "Well sorted!", "$score of $goal right  +$score ⭐", { n = 0; score = 0; cur = next(); chosen = null; done = false }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("Sort it!", onBack, "⭐ $score/$goal")
            Text("Where does it go?", color = SunYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(cur.first.e, fontSize = 110.sp, modifier = Modifier.clickable { s.say(cur.first.name) })
            Text(cur.first.name, color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf(true to d.pack, false to d.pack2).forEach { (isA, pk) ->
                    val bg = when {
                        chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                        isA == cur.second -> Brush.linearGradient(listOf(Good, Good))
                        isA == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                    }
                    Column(
                        Modifier.weight(1f).padding(6.dp).height(120.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable { pick(isA) },
                        Arrangement.Center, Alignment.CenterHorizontally
                    ) {
                        Text(Packs.items(pk).first().e, fontSize = 40.sp)
                        Text(Packs.title(pk), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

// =============================================================== 14-17. Skill games (classic / race / streak / true-false) from the big syllabus bank
@Composable
fun SkillGame(s: AppState, d: GameDef, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val mode = d.kind
    val total = if (mode == GK.SK_TF) 10 else 8
    var q by remember { mutableStateOf(Bank.nextBySkill(profile.level, d.skill)) }
    var proposed by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var asked by remember { mutableIntStateOf(1) }
    var lives by remember { mutableIntStateOf(3) }
    var left by remember { mutableIntStateOf(60) }
    var done by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }

    fun propose(nq: Question) { proposed = if (Random.nextBoolean()) nq.answer else (nq.options.filter { it != nq.answer }.randomOrNull() ?: nq.answer) }
    LaunchedEffect(round) {
        while (q == null) { delay(300); q = Bank.nextBySkill(profile.level, d.skill) }
        q?.let { propose(it) }
    }
    LaunchedEffect(round) {
        if (mode == GK.SK_RACE) { while (left > 0 && !done) { delay(1000); left-- }; done = true }
    }
    LaunchedEffect(q, proposed) {
        val qq = q
        if (qq != null && mode != GK.SK_RACE && !done) s.say(if (mode == GK.SK_TF) qq.speak + " ... " + proposed + "?" else qq.speak)
    }
    fun judge(ok: Boolean) {
        val qq = q ?: return
        reward(s, qq.skill, qq.label, ok)
        if (ok) { score++; confetti++ } else if (mode == GK.SK_STREAK) lives--
        if (mode != GK.SK_RACE) s.say(if (ok) Content.cheer() else "Good try! The answer is ${qq.answer}.")
    }
    LaunchedEffect(chosen) {
        if (chosen != null) {
            delay(if (mode == GK.SK_RACE) 450L else 1500L)
            val finished = when (mode) { GK.SK_STREAK -> lives <= 0; GK.SK_RACE -> left <= 0; else -> asked >= total }
            if (finished) done = true else {
                val nq = Bank.nextBySkill(profile.level, d.skill)
                q = nq; chosen = null; asked++
                if (nq != null) propose(nq)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val cur = q
        if (done) DoneView(if (score >= total / 2) "🏆" else "💪", "$score correct!", "+$score ⭐", {
            score = 0; asked = 1; lives = 3; left = 60; done = false; chosen = null; q = Bank.nextBySkill(profile.level, d.skill); round++
        }, onBack)
        else Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(d.title, onBack, when (mode) { GK.SK_RACE -> "⏱ $left  ⭐ $score"; GK.SK_STREAK -> "${"♥".repeat(maxOf(lives, 0))}  ⭐ $score"; else -> "$asked/$total  ⭐ $score" })
            if (cur == null) Text("Loading questions…", color = Color.White, fontSize = 18.sp)
            else {
                GlassCard(Modifier.fillMaxWidth().clickable { s.say(cur.speak) }) {
                    if (cur.visual.isNotEmpty()) Text(cur.visual, fontSize = 40.sp, textAlign = TextAlign.Center, color = Color.White)
                    Text(cur.prompt, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (mode == GK.SK_TF) Text("→ $proposed ?", color = SunYellow, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                if (mode == GK.SK_TF) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf(true to "✔ True", false to "✖ False").forEach { (v, label) ->
                            val truth = proposed == cur.answer
                            val bg = when {
                                chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                                v == truth -> Brush.linearGradient(listOf(Good, Good))
                                else -> Brush.linearGradient(listOf(Bad, Bad))
                            }
                            Box(Modifier.weight(1f).padding(6.dp).height(100.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable {
                                if (chosen == null) { chosen = label; judge(v == truth) }
                            }, contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                } else {
                    cur.options.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { opt ->
                                val bg = when {
                                    chosen == null -> Brush.linearGradient(listOf(Purple.copy(alpha = 0.85f), Color(0xFF5E35B1)))
                                    opt == cur.answer -> Brush.linearGradient(listOf(Good, Good))
                                    opt == chosen -> Brush.linearGradient(listOf(Bad, Bad))
                                    else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                                }
                                Box(Modifier.weight(1f).padding(6.dp).height(84.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable {
                                    if (chosen == null) { chosen = opt; judge(opt == cur.answer) }
                                }, contentAlignment = Alignment.Center) {
                                    Text(opt, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}
