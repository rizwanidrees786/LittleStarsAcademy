package com.rizwan.kidslearn

import android.content.Context
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln
import kotlin.random.Random

// =============================================================== Syllabus knowledge base (assets/kb/core.tsv)
data class Fact(val a: String, val score: Float)

object KnowledgeBase {
    @Volatile var ready = false
    @Volatile private var started = false
    private var answers = ArrayList<String>()
    private var qtoks = ArrayList<List<String>>()
    private var index = HashMap<String, MutableList<Int>>()
    private var weight = HashMap<String, Float>()
    private val stop = setOf(
        "what", "whats", "is", "are", "the", "a", "an", "of", "in", "to", "me", "tell", "about", "how", "do", "does",
        "you", "can", "i", "my", "it", "was", "please", "say", "who", "which", "where", "when", "why", "and", "for",
        "on", "at", "with", "this", "that", "we", "our", "your", "kya", "hai", "ko", "ka", "ki", "ke", "mein", "se", "kehte", "hain"
    )

    private fun stem(w: String) = if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) w.dropLast(1) else w

    fun tokens(t: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (ch in t.lowercase().replace("'", "")) {
            if (ch.isLetterOrDigit()) sb.append(ch)
            else if (sb.isNotEmpty()) { out.add(sb.toString()); sb.setLength(0) }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out.filter { it !in stop }.map { stem(it) }
    }

    fun load(ctx: Context) {
        if (started) return
        started = true
        Thread {
            try {
                val a = ArrayList<String>()
                val qt = ArrayList<List<String>>()
                val idx = HashMap<String, MutableList<Int>>()
                ctx.assets.open("kb/core.tsv").bufferedReader(Charsets.UTF_8).useLines { seq ->
                    seq.forEach { line ->
                        val f = line.split('\t')
                        if (f.size >= 3) {
                            val toks = tokens(f[1]).distinct()
                            if (toks.isNotEmpty()) {
                                val id = a.size
                                a.add(f[2]); qt.add(toks)
                                toks.forEach { idx.getOrPut(it) { ArrayList() }.add(id) }
                            }
                        }
                    }
                }
                val n = a.size.toFloat()
                val w = HashMap<String, Float>()
                idx.forEach { (k, v) -> w[k] = ln(1f + n / v.size) }
                answers = a; qtoks = qt; index = idx; weight = w
                ready = true
            } catch (_: Throwable) { }
        }.start()
    }

    fun search(query: String, k: Int = 3): List<Fact> {
        if (!ready) return emptyList()
        val qt = tokens(query).distinct()
        if (qt.isEmpty()) return emptyList()
        val shared = HashMap<Int, Float>()
        for (t in qt) {
            val ids = index[t] ?: continue
            val w = weight[t] ?: 1f
            for (id in ids) shared[id] = (shared[id] ?: 0f) + w
        }
        if (shared.isEmpty()) return emptyList()
        var qW = 0f
        for (t in qt) qW += weight[t] ?: 1.5f
        val scored = ArrayList<Pair<Int, Float>>()
        for ((id, s) in shared) {
            var fW = 0f
            for (t in qtoks[id]) fW += weight[t] ?: 1f
            scored.add(id to (2f * s / (qW + fW)))
        }
        scored.sortByDescending { it.second }
        val out = ArrayList<Fact>()
        val seenA = HashSet<String>()
        for ((id, sc) in scored) {
            val a = answers[id]
            if (seenA.add(a)) out.add(Fact(a, sc))
            if (out.size >= k) break
        }
        return out
    }
}

// =============================================================== Instant, always-correct maths
object MathSolver {
    private val small = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
        "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    private val tensW = mapOf("twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90)

    fun numWord(n: Int): String = when {
        n < 0 -> "minus " + numWord(-n)
        n < 20 -> small[n]
        n < 100 -> tensW.entries.first { it.value == n / 10 * 10 }.key + (if (n % 10 != 0) "-" + small[n % 10] else "")
        n < 1000 -> small[n / 100] + " hundred" + (if (n % 100 != 0) " " + numWord(n % 100) else "")
        else -> n.toString()
    }

    private fun wordsToDigits(t: String): String {
        val parts = t.split(Regex("\\s+"))
        val out = ArrayList<String>()
        var i = 0
        while (i < parts.size) {
            val p = parts[i].trim('?', '.', ',', '!')
            val v = small.indexOf(p).takeIf { it >= 0 } ?: tensW[p]
            if (v != null) {
                var total = v
                if (tensW.containsKey(p) && i + 1 < parts.size) {
                    val nxt = small.indexOf(parts[i + 1].trim('?', '.', ',', '!'))
                    if (nxt in 1..9) { total += nxt; i++ }
                }
                out.add(total.toString())
            } else out.add(parts[i])
            i++
        }
        return out.joinToString(" ")
    }

    private fun num(s: String): Int? = s.toIntOrNull()?.takeIf { it in 0..100000 }

    fun solve(text: String): String? {
        var t = text.lowercase().replace("×", "x").replace("−", "-").replace("÷", "/").trim()
        if (Regex("\\b(plus|minus|times|add|subtract|multiplied|divided|take away)\\b").containsMatchIn(t)) t = wordsToDigits(t)

        Regex("(\\d+)\\s*(?:\\+|plus)\\s*(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null) return "$a plus $b is ${a + b}! ${numWord(a)} plus ${numWord(b)} equals ${numWord(a + b)}."
        }
        Regex("add\\s+(\\d+)\\s+(?:and|to|with)\\s+(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null) return "$a plus $b is ${a + b}! ${numWord(a)} plus ${numWord(b)} equals ${numWord(a + b)}."
        }
        Regex("(?:take away|subtract)\\s+(\\d+)\\s+from\\s+(\\d+)").find(t)?.let { m ->
            val b = num(m.groupValues[1]); val a = num(m.groupValues[2])
            if (a != null && b != null) return "$a minus $b is ${a - b}! ${numWord(a)} minus ${numWord(b)} equals ${numWord(a - b)}."
        }
        Regex("(\\d+)\\s*(?:-|minus)\\s*(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null) return "$a minus $b is ${a - b}! ${numWord(a)} minus ${numWord(b)} equals ${numWord(a - b)}."
        }
        Regex("(\\d+)\\s*(?:x|\\*|times|multiplied by)\\s*(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null) return "$a times $b is ${a * b}! ${numWord(a)} times ${numWord(b)} equals ${numWord(a * b)}."
        }
        Regex("(\\d+)\\s*(?:/|divided by)\\s*(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null) {
                if (b == 0) return "We cannot divide by zero!"
                return if (a % b == 0) "$a divided by $b is ${a / b}!" else "$a divided by $b is ${a / b} with ${a % b} left over."
            }
        }
        Regex("between\\s+(\\d+)\\s+and\\s+(\\d+)").find(t)?.let { m ->
            val a = num(m.groupValues[1]); val b = num(m.groupValues[2])
            if (a != null && b != null && b - a == 2) return "${a + 1} comes between $a and $b."
        }
        Regex("after\\s+(\\d+)").find(t)?.let { m -> num(m.groupValues[1])?.let { return "${it + 1} comes after $it." } }
        Regex("before\\s+(\\d+)").find(t)?.let { m -> num(m.groupValues[1])?.let { if (it >= 1) return "${it - 1} comes before $it." } }
        Regex("count\\s+(?:to|till|up to|until)\\s+(\\d+)").find(t)?.let { m ->
            val n = num(m.groupValues[1]) ?: 10
            val k = if (n in 1..50) n else 10
            return (1..k).joinToString(", ") + ". Great counting!"
        }
        Regex("(\\d+)\\s*(?:in words|number name)|spell\\s+(\\d+)|write\\s+(\\d+)\\s+in words|name of (?:number )?(\\d+)").find(t)?.let { m ->
            val n = m.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.let { num(it) }
            if (n != null) return "$n is written ${numWord(n)}."
        }
        if (Regex("\\b(bigger|larger|greater|biggest|largest|smaller|smallest|less|lesser)\\b").containsMatchIn(t)) {
            val ns = Regex("\\d+").findAll(t).mapNotNull { num(it.value) }.toList()
            if (ns.size >= 2) {
                val big = Regex("bigger|larger|greater|biggest|largest").containsMatchIn(t)
                val r = if (big) ns.max() else ns.min()
                return "$r is ${if (big) "bigger" else "smaller"}! (${ns.joinToString(", ")})"
            }
        }
        if (Regex("\\b(even|odd)\\b").containsMatchIn(t)) {
            val n = Regex("\\d+").find(t)?.value?.let { num(it) }
            if (n != null) return "$n is ${if (n % 2 == 0) "even" else "odd"}."
        }
        return null
    }
}

// =============================================================== Syllabus books (assets/kb/curriculum.json)
class Page(val t: String, val e: String)
class PracticeSpec(val level: Int, val skill: String, val labels: Set<String>)
class LessonDef(
    val id: String, val level: Int, val subject: String, val unit: String, val title: String, val emoji: String,
    val pages: List<Page>, val practice: List<PracticeSpec>, val urdu: Boolean
)

object Curriculum {
    @Volatile var all: List<LessonDef> = emptyList()
    private var started = false

    fun load(ctx: Context) {
        if (started) return
        started = true
        Thread {
            try {
                val txt = ctx.assets.open("kb/curriculum.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val arr = JSONArray(txt)
                val out = ArrayList<LessonDef>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val pg = o.getJSONArray("pages")
                    val pages = List(pg.length()) { j -> pg.getJSONObject(j).let { Page(it.getString("t"), it.getString("e")) } }
                    val pr = o.getJSONArray("practice")
                    val practice = List(pr.length()) { j ->
                        val p = pr.getJSONObject(j)
                        val la = p.getJSONArray("labels")
                        val set = HashSet<String>()
                        for (x in 0 until la.length()) set.add(la.getString(x))
                        PracticeSpec(p.getInt("lv"), p.getString("skill"), set)
                    }
                    out.add(LessonDef(o.getString("id"), o.getInt("level"), o.getString("subject"), o.getString("unit"), o.getString("title"),
                        o.getString("emoji"), pages, practice, o.optBoolean("urdu", false)))
                }
                all = out
            } catch (_: Throwable) { }
        }.start()
    }
}

// =============================================================== Stories and riddles library (assets/kb/stories.tsv, riddles.tsv)
class RiddleItem(val clue: String, val answer: String, val emoji: String, val clueUr: String, val ansUr: String)

object Library {
    @Volatile var stories: List<Story> = emptyList()
    @Volatile var riddles: List<RiddleItem> = emptyList()
    private var started = false

    fun load(ctx: Context) {
        if (started) return
        started = true
        Thread {
            try {
                val st = ArrayList<Story>()
                ctx.assets.open("kb/stories.tsv").bufferedReader(Charsets.UTF_8).useLines { seq ->
                    seq.forEach { line ->
                        val f = line.split('\t')
                        if (f.size >= 6) {
                            val lines = f[3].split('‖').map { x -> val p = x.split('¦'); Line(p[0], p.getOrElse(1) { "" }) }.toMutableList()
                            lines.add(Line("Moral: " + f[4], "سبق: " + f[5]))
                            st.add(Story(f[0] + "  ·  " + f[1], f[2], lines))
                        }
                    }
                }
                stories = st
                val rd = ArrayList<RiddleItem>()
                ctx.assets.open("kb/riddles.tsv").bufferedReader(Charsets.UTF_8).useLines { seq ->
                    seq.forEach { line ->
                        val f = line.split('\t')
                        if (f.size >= 5) rd.add(RiddleItem(f[0], f[1], f[2], f[3], f[4]))
                    }
                }
                riddles = rd
            } catch (_: Throwable) { }
        }.start()
    }
}

// =============================================================== Big practice bank (assets/kb/bank_N.tsv; 1=PG, 2=KG, 3=Class One)
object Bank {
    private class Data(val lines: List<String>, val bySkill: Map<String, IntArray>)
    private val cache = ConcurrentHashMap<Int, Data>()
    private val loading = ConcurrentHashMap.newKeySet<Int>()
    private val lessonPools = ConcurrentHashMap<String, IntArray>()

    fun isLoaded(level: Int) = cache.containsKey(level)

    fun load(ctx: Context, level: Int) {
        if (cache.containsKey(level) || !loading.add(level)) return
        Thread {
            try {
                val lines = ArrayList<String>(50000)
                val map = HashMap<String, MutableList<Int>>()
                ctx.assets.open("kb/bank_$level.tsv").bufferedReader(Charsets.UTF_8).useLines { seq ->
                    seq.forEach { line ->
                        val tab = line.indexOf('\t')
                        if (tab > 0) {
                            map.getOrPut(line.substring(0, tab)) { ArrayList() }.add(lines.size)
                            lines.add(line)
                        }
                    }
                }
                cache[level] = Data(lines, map.mapValues { it.value.toIntArray() })
            } catch (_: Throwable) { }
            loading.remove(level)
        }.start()
    }

    /** Picks a skill (weak skills more often), then a random question of that skill. */
    fun next(level: Int, p: Progress): Question? {
        val d = cache[level] ?: return null
        val skills = d.bySkill.keys.toList()
        if (skills.isEmpty()) return null
        val weights = skills.map { sk ->
            val a = p.attempts[sk] ?: 0
            val c = p.correct[sk] ?: 0
            1.0 + 3.0 * (if (a >= 2) (a - c).toDouble() / a else 0.3)
        }
        var r = Random.nextDouble() * weights.sum()
        var pick = skills.last()
        for (i in skills.indices) { r -= weights[i]; if (r <= 0) { pick = skills[i]; break } }
        val arr = d.bySkill[pick] ?: return null
        return parse(d.lines[arr[Random.nextInt(arr.size)]])
    }

    fun skills(level: Int): List<String> = cache[level]?.bySkill?.keys?.sorted() ?: emptyList()

    fun nextBySkill(level: Int, skill: String): Question? {
        val d = cache[level] ?: return null
        val arr = d.bySkill[skill] ?: return null
        return parse(d.lines[arr[Random.nextInt(arr.size)]])
    }

    private fun labelOf(line: String): String {
        val a = line.indexOf('\t')
        val b = line.indexOf('\t', a + 1)
        return if (a >= 0 && b > a) line.substring(a + 1, b) else ""
    }

    private fun poolOf(l: LessonDef): IntArray {
        lessonPools[l.id]?.let { return it }
        val codes = ArrayList<Int>()
        for (p in l.practice) {
            val d = cache[p.level] ?: return IntArray(0)
            val arr = d.bySkill[p.skill] ?: continue
            for (i in arr) if (p.labels.contains(labelOf(d.lines[i]))) codes.add(p.level * 10_000_000 + i)
        }
        val res = codes.toIntArray()
        if (res.isNotEmpty()) lessonPools[l.id] = res
        return res
    }

    /** Questions that belong to exactly this lesson (empty while its bank is still loading). */
    fun lessonQuestions(l: LessonDef, count: Int): List<Question> {
        val arr = poolOf(l)
        if (arr.isEmpty()) return emptyList()
        val out = ArrayList<Question>()
        val seen = HashSet<String>()
        var tries = 0
        while (out.size < count && tries < count * 40) {
            tries++
            val code = arr[Random.nextInt(arr.size)]
            val d = cache[code / 10_000_000] ?: continue
            val q = parse(d.lines[code % 10_000_000]) ?: continue
            if (seen.add(q.prompt + q.visual)) out.add(q)
        }
        return out
    }

    private fun parse(line: String): Question? {
        val f = line.split('\t')
        if (f.size < 6) return null
        val prompt = f[3]
        val spoken = when {
            f[0].startsWith("Urdu") && hasArabic(prompt) -> prompt
            else -> f.getOrNull(6)?.takeIf { it.isNotBlank() }
                ?: prompt.filter { it.code < 128 }.replace(Regex("\\s+"), " ").trim().ifBlank { prompt }
        }
        return Question(f[0], f[1], prompt, f[2], spoken, f[5].split('|'), f[4])
    }
}
