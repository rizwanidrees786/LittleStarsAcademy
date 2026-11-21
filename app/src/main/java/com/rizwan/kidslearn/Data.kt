package com.rizwan.kidslearn

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** A friendly original character (24 total). */
data class Buddy(
    val id: String, val name: String, val title: String, val emoji: String,
    val c1: Color, val c2: Color, val pitch: Float
)

private fun b(id: String, name: String, title: String, e: String, a: Long, c: Long, p: Float) =
    Buddy(id, name, title, e, Color(a), Color(c), p)

val BUDDIES = listOf(
    b("leo", "Leo", "the Lion Cub", "🦁", 0xFFFFB300, 0xFFFF6F00, 0.9f),
    b("rosy", "Rosy", "the Unicorn", "🦄", 0xFFFF80AB, 0xFFB388FF, 1.3f),
    b("bobo", "Bobo", "the Bear", "🐻", 0xFF8D6E63, 0xFFFFB74D, 0.8f),
    b("pinky", "Pinky", "the Bunny", "🐰", 0xFFF48FB1, 0xFFCE93D8, 1.4f),
    b("bubbles", "Bubbles", "the Dolphin", "🐬", 0xFF4FC3F7, 0xFF0288D1, 1.2f),
    b("zoya", "Zoya", "the Fox", "🦊", 0xFFFF8A65, 0xFFE64A19, 1.2f),
    b("pogo", "Pogo", "the Panda", "🐼", 0xFF90A4AE, 0xFF37474F, 0.95f),
    b("kiki", "Kiki", "the Kitten", "🐱", 0xFFFFCC80, 0xFFFF7043, 1.4f),
    b("tommy", "Tommy", "the Turtle", "🐢", 0xFF81C784, 0xFF2E7D32, 0.85f),
    b("dani", "Dani", "the Dino", "🦖", 0xFF9CCC65, 0xFF00897B, 0.8f),
    b("penny", "Penny", "the Penguin", "🐧", 0xFF64B5F6, 0xFF1A237E, 1.2f),
    b("ollie", "Ollie", "the Owl", "🦉", 0xFFA1887F, 0xFF5D4037, 0.9f),
    b("mia", "Mia", "the Monkey", "🐵", 0xFFFFD54F, 0xFF8D6E63, 1.3f),
    b("ellie", "Ellie", "the Elephant", "🐘", 0xFF9FA8DA, 0xFF3949AB, 0.85f),
    b("freddy", "Freddy", "the Frog", "🐸", 0xFF69F0AE, 0xFF00C853, 1.1f),
    b("gigi", "Gigi", "the Giraffe", "🦒", 0xFFFFE082, 0xFFFF8F00, 1.2f),
    b("hana", "Hana", "the Hedgehog", "🦔", 0xFFBCAAA4, 0xFF6D4C41, 1.25f),
    b("chirpy", "Chirpy", "the Chick", "🐥", 0xFFFFF176, 0xFFFFA000, 1.5f),
    b("ziggy", "Ziggy", "the Zebra", "🦓", 0xFFB0BEC5, 0xFF455A64, 1.1f),
    b("sunny", "Sunny", "the Star", "⭐", 0xFFFFEB3B, 0xFFFF9800, 1.3f),
    b("rafi", "Robo Rafi", "the Robot", "🤖", 0xFF80DEEA, 0xFF5E35B1, 0.75f),
    b("noor", "Noor", "the Fairy", "🧚", 0xFFF8BBD0, 0xFF7E57C2, 1.4f),
    b("kaleem", "Captain Kaleem", "the Hero", "🦸", 0xFFEF5350, 0xFF1565C0, 0.85f),
    b("mina", "Mina", "the Mermaid", "🧜", 0xFF4DD0E1, 0xFFAB47BC, 1.3f),
)

fun buddyById(id: String?): Buddy = BUDDIES.firstOrNull { it.id == id } ?: BUDDIES[0]

/** Data belongs to the child's NAME/profile, never to the character. */
data class Profile(val id: String, val name: String, val level: Int, val grade: String, val defaultBuddy: String)

val PROFILES = listOf(
    Profile("aliha", "Aliha", 3, "KG-II · 7 years", "rosy"),
    Profile("affan", "M Affan", 2, "KG-I · 5 years", "leo"),
    Profile("aenan", "Aenan", 1, "Little Learner · 3 years", "bobo"),
)

class Progress {
    val attempts = mutableMapOf<String, Int>()
    val correct = mutableMapOf<String, Int>()
    val misses = mutableMapOf<String, Int>()          // weak items, e.g. "Letter B" -> 3
    val tests = mutableListOf<Pair<Long, Int>>()      // (time, score out of 10)
    var stars = 0

    fun record(q: Question, ok: Boolean) {
        attempts[q.skill] = (attempts[q.skill] ?: 0) + 1
        if (ok) {
            correct[q.skill] = (correct[q.skill] ?: 0) + 1
            val m = misses[q.label] ?: 0
            if (m > 1) misses[q.label] = m - 1 else misses.remove(q.label)   // improvement removes weakness
        } else {
            misses[q.label] = (misses[q.label] ?: 0) + 1
        }
    }

    fun totalAttempts() = attempts.values.sum()
    fun totalCorrect() = correct.values.sum()

    fun toJson(): String {
        val o = JSONObject()
        o.put("stars", stars)
        o.put("att", JSONObject(attempts as Map<*, *>))
        o.put("cor", JSONObject(correct as Map<*, *>))
        o.put("mis", JSONObject(misses as Map<*, *>))
        val t = JSONArray()
        tests.forEach { t.put(JSONArray(listOf(it.first, it.second))) }
        o.put("tests", t)
        return o.toString()
    }

    companion object {
        fun fromJson(s: String?): Progress {
            val p = Progress()
            if (s.isNullOrEmpty()) return p
            try {
                val o = JSONObject(s)
                p.stars = o.optInt("stars")
                fun read(name: String, into: MutableMap<String, Int>) {
                    val j = o.optJSONObject(name) ?: return
                    j.keys().forEach { k -> into[k] = j.getInt(k) }
                }
                read("att", p.attempts); read("cor", p.correct); read("mis", p.misses)
                val t = o.optJSONArray("tests")
                if (t != null) for (i in 0 until t.length()) {
                    val x = t.getJSONArray(i)
                    p.tests.add(x.getLong(0) to x.getInt(1))
                }
            } catch (_: Exception) { }
            return p
        }
    }
}

class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("stars_academy", Context.MODE_PRIVATE)
    fun buddyId(profile: Profile): String = prefs.getString("buddy_${profile.id}", profile.defaultBuddy) ?: profile.defaultBuddy
    fun setBuddy(pid: String, buddyId: String) = prefs.edit().putString("buddy_$pid", buddyId).apply()
    fun progress(pid: String) = Progress.fromJson(prefs.getString("prog_$pid", null))
    fun saveProgress(pid: String, p: Progress) = prefs.edit().putString("prog_$pid", p.toJson()).apply()
    fun resetProgress(pid: String) = prefs.edit().remove("prog_$pid").apply()
    fun pin(): String = prefs.getString("pin", "1234") ?: "1234"
    fun setPin(p: String) = prefs.edit().putString("pin", p).apply()
}
