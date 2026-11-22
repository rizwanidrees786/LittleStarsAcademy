package com.rizwan.kidslearn

import kotlin.random.Random

data class Question(
    val skill: String, val label: String, val prompt: String, val visual: String,
    val speak: String, val options: List<String>, val answer: String
)

data class Line(val en: String, val ur: String)
data class Story(val title: String, val emoji: String, val lines: List<Line>)

object Content {
    const val LETTERS = "Letters"; const val COLORS = "Colors"; const val SHAPES = "Shapes"
    const val COUNTING = "Counting"; const val ADD = "Addition"; const val SUB = "Subtraction"
    const val WORDS = "Words"; const val OPP = "Opposites"; const val URDU = "Urdu → English"

    fun skillsFor(level: Int): List<String> = when (level) {
        1 -> listOf(LETTERS, COLORS, SHAPES, COUNTING, URDU)
        2 -> listOf(LETTERS, COUNTING, ADD, WORDS, SHAPES, URDU)
        else -> listOf(WORDS, LETTERS, ADD, SUB, OPP, COUNTING, URDU)
    }

    private val alphabet = listOf(
        Triple("A", "Apple", "🍎"), Triple("B", "Ball", "⚽"), Triple("C", "Cat", "🐱"), Triple("D", "Dog", "🐶"),
        Triple("E", "Elephant", "🐘"), Triple("F", "Fish", "🐟"), Triple("G", "Grapes", "🍇"), Triple("H", "Hat", "🎩"),
        Triple("I", "Ice cream", "🍦"), Triple("J", "Juice", "🧃"), Triple("K", "Key", "🔑"), Triple("L", "Lion", "🦁"),
        Triple("M", "Moon", "🌙"), Triple("N", "Nose", "👃"), Triple("O", "Orange", "🍊"), Triple("P", "Pizza", "🍕"),
        Triple("Q", "Queen", "👸"), Triple("R", "Rainbow", "🌈"), Triple("S", "Sun", "☀️"), Triple("T", "Tree", "🌳"),
        Triple("U", "Umbrella", "☂️"), Triple("V", "Van", "🚐"), Triple("W", "Watch", "⌚"), Triple("X", "Xylophone", "🎶"),
        Triple("Y", "Yacht", "⛵"), Triple("Z", "Zebra", "🦓")
    )
    private val colors = listOf("Red" to "🔴", "Blue" to "🔵", "Green" to "🟢", "Yellow" to "🟡", "Orange" to "🟠",
        "Purple" to "🟣", "Brown" to "🟤", "Black" to "⚫", "White" to "⚪")
    private val shapes = listOf("Star" to "⭐", "Heart" to "❤️", "Triangle" to "🔺", "Square" to "🟦", "Circle" to "⚪", "Diamond" to "🔷")
    private val cvc = listOf("Cat" to "🐱", "Dog" to "🐶", "Sun" to "☀️", "Bus" to "🚌", "Hat" to "🎩", "Pig" to "🐷",
        "Cup" to "☕", "Bed" to "🛏️", "Fox" to "🦊", "Bat" to "🦇", "Pen" to "🖊️", "Hen" to "🐔", "Jet" to "✈️", "Bag" to "🎒")
    private val opposites = listOf("Big" to "Small", "Hot" to "Cold", "Up" to "Down", "Happy" to "Sad", "Day" to "Night",
        "Fast" to "Slow", "Open" to "Close", "Old" to "New", "Hard" to "Soft", "Full" to "Empty")
    val urdu = listOf(
        Triple("Paani", "پانی", "Water"), Triple("Kitaab", "کتاب", "Book"), Triple("Billi", "بلی", "Cat"),
        Triple("Sooraj", "سورج", "Sun"), Triple("Chaand", "چاند", "Moon"), Triple("Doodh", "دودھ", "Milk"),
        Triple("Phool", "پھول", "Flower"), Triple("Darakht", "درخت", "Tree"), Triple("Machli", "مچھلی", "Fish"),
        Triple("Aam", "آم", "Mango"), Triple("Ghar", "گھر", "House"), Triple("Roti", "روٹی", "Bread")
    )
    private val countEmoji = listOf("🍎", "⭐", "🐟", "🎈", "🍓", "🐥")
    private val cheers = listOf("Great job!", "Wow, you are a star!", "Excellent!", "Super smart!", "Well done!", "Fantastic!")
    fun cheer() = cheers.random()

    private fun opts(answer: String, pool: List<String>, n: Int = 4): List<String> =
        (listOf(answer) + pool.filter { it != answer }.shuffled().take(n - 1)).shuffled()

    private fun numOpts(ans: Int, max: Int): List<String> {
        val s = mutableSetOf(ans)
        while (s.size < 4) s.add((ans + Random.nextInt(-3, 4)).coerceIn(0, max + 3))
        return s.shuffled().map { it.toString() }
    }

    /** Picks the next skill; skills where the child makes more mistakes appear more often. */
    fun next(level: Int, p: Progress): Question {
        if (Random.nextInt(100) < 80) Bank.next(level, p)?.let { return it }
        val skills = skillsFor(level)
        val weights = skills.map { sk ->
            val a = p.attempts[sk] ?: 0
            val c = p.correct[sk] ?: 0
            1.0 + 3.0 * (if (a >= 2) (a - c).toDouble() / a else 0.3)
        }
        var r = Random.nextDouble() * weights.sum()
        var pick = skills.last()
        for (i in skills.indices) { r -= weights[i]; if (r <= 0) { pick = skills[i]; break } }
        return make(pick, level)
    }

    private fun make(skill: String, level: Int): Question = when (skill) {
        LETTERS -> {
            val (l, w, e) = alphabet.random()
            Question(skill, "Letter $l", "Which letter does $w start with?", e,
                "Which letter does $w start with? $w.", opts(l, alphabet.map { it.first }), l)
        }
        COLORS -> {
            val (n, e) = colors.random()
            Question(skill, "Color $n", "What color is this?", e, "What color is this?", opts(n, colors.map { it.first }), n)
        }
        SHAPES -> {
            val (n, e) = shapes.random()
            Question(skill, "Shape $n", "What shape is this?", e, "What shape is this?", opts(n, shapes.map { it.first }), n)
        }
        COUNTING -> {
            val max = when (level) { 1 -> 5; 2 -> 10; else -> 20 }
            val n = Random.nextInt(1, max + 1)
            Question(skill, "Counting $n", "How many can you count?", countEmoji.random().repeat(n),
                "How many can you count?", numOpts(n, max), n.toString())
        }
        ADD -> {
            val m = if (level <= 2) 5 else 10
            val a = Random.nextInt(1, m + 1); val b = Random.nextInt(1, m + 1)
            Question(skill, "Add $a + $b", "Add the numbers", "$a + $b = ?", "What is $a plus $b?", numOpts(a + b, 2 * m), (a + b).toString())
        }
        SUB -> {
            val a = Random.nextInt(2, 16); val b = Random.nextInt(1, a + 1)
            Question(skill, "Subtract $a - $b", "Take away", "$a − $b = ?", "What is $a minus $b?", numOpts(a - b, 15), (a - b).toString())
        }
        WORDS -> {
            val (w, e) = cvc.random()
            Question(skill, "Word $w", "Which word matches the picture?", e, "Which word matches the picture?", opts(w, cvc.map { it.first }), w)
        }
        OPP -> {
            val (a, b) = opposites.random()
            Question(skill, "Opposite $a", "What is the opposite of $a?", "↔️", "What is the opposite of $a?", opts(b, opposites.map { it.second }), b)
        }
        else -> {
            val (r, s, en) = urdu.random()
            Question(URDU, "Urdu $r", "$r  ($s)  in English?", "🇵🇰 → 🇬🇧", "What is $r in English?", opts(en, urdu.map { it.third }), en)
        }
    }

    val stories = listOf(
        Story("The Thirsty Crow", "🐦", listOf(
            Line("A crow was very thirsty.", "ایک کوا بہت پیاسا تھا۔"),
            Line("He found a jar with a little water.", "اسے ایک مٹکا ملا جس میں تھوڑا سا پانی تھا۔"),
            Line("He dropped small stones into the jar.", "اس نے مٹکے میں چھوٹے چھوٹے کنکر ڈالے۔"),
            Line("The water came up and the crow drank. Never give up!", "پانی اوپر آ گیا اور کوے نے پی لیا۔ ہمت نہ ہارو!"))),
        Story("The Kind Lion Cub", "🦁", listOf(
            Line("Leo the lion cub found a lost bunny.", "شیر کے بچے لیو کو ایک کھویا ہوا خرگوش ملا۔"),
            Line("The bunny was crying.", "خرگوش رو رہا تھا۔"),
            Line("Leo said, Don't cry, I will help you.", "لیو نے کہا، رو مت، میں تمہاری مدد کروں گا۔"),
            Line("They found mama bunny. Being kind makes everyone happy!", "انہوں نے خرگوش کی امی کو ڈھونڈ لیا۔ مہربانی سب کو خوش کرتی ہے!"))),
        Story("Moon and Stars", "🌙", listOf(
            Line("At night, the moon smiles in the sky.", "رات کو چاند آسمان پر مسکراتا ہے۔"),
            Line("Little stars dance around the moon.", "چھوٹے ستارے چاند کے گرد ناچتے ہیں۔"),
            Line("Birds say good night and go to sleep.", "پرندے شب بخیر کہہ کر سو جاتے ہیں۔"),
            Line("Now it is time for you to sleep too. Good night!", "اب آپ کے سونے کا وقت ہے۔ شب بخیر!"))),
        Story("Tommy Turtle's Race", "🐢", listOf(
            Line("Tommy Turtle was slow but never gave up.", "ٹومی کچھوا سست تھا مگر ہمت نہیں ہارتا تھا۔"),
            Line("A fast rabbit laughed at him.", "ایک تیز خرگوش اس پر ہنسا۔"),
            Line("The rabbit took a nap during the race.", "خرگوش دوڑ میں سو گیا۔"),
            Line("Tommy walked slowly and won. Slow and steady wins!", "ٹومی آہستہ آہستہ چلا اور جیت گیا۔ آہستہ مگر مسلسل چلنے والا جیتتا ہے!")))
    )

    val jokes = listOf(
        "Why did the banana go to the doctor? Because it was not peeling well!",
        "What is a cat's favorite color? Purr-ple!",
        "Why was the math book sad? It had too many problems!",
        "What do you call a sleeping bull? A bulldozer!",
        "Why do cows wear bells? Because their horns do not work!",
        "What do you call a fish with no eyes? A fsh!",
        "Why did the cookie go to the doctor? Because it felt crumbly!",
        "What is a bee's favorite hair style? A buzz cut!"
    )

    val riddles = listOf(
        "I am yellow and I shine in the sky in the day. Who am I?" to "The Sun",
        "I have four legs and I say meow. Who am I?" to "A Cat",
        "I am a yellow fruit. Monkeys love me. What am I?" to "A Banana",
        "I fall from the clouds and make you wet. What am I?" to "Rain",
        "I have two wheels and you pedal me. What am I?" to "A Bicycle",
        "I am white. Cows give me and you drink me. What am I?" to "Milk"
    )
}
