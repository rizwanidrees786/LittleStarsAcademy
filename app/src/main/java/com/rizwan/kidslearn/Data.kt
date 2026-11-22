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
    b("mimo", "Mimo", "the Cat", "🐱", 0xFFFFB74D, 0xFFFF7043, 1.45f),
    b("bruno", "Bruno", "the Dog", "🐶", 0xFFD7A86E, 0xFF8D5524, 0.9f),
    b("bella", "Bella", "the Bunny", "🐰", 0xFFF8BBD0, 0xFFCE93D8, 1.5f),
    b("teddy", "Teddy", "the Bear", "🐻", 0xFFBCAAA4, 0xFF795548, 0.75f),
    b("pogo", "Pogo", "the Panda", "🐼", 0xFFB0BEC5, 0xFF546E7A, 0.9f),
    b("kiwi", "Kiwi", "the Parrot", "🦜", 0xFF69F0AE, 0xFF00897B, 1.6f),
    b("gloop", "Gloop", "the Slime", "🟢", 0xFFB9F6CA, 0xFF2E7D32, 1.25f),
    b("blu", "Blu", "the Slime", "🔵", 0xFF81D4FA, 0xFF1565C0, 1.15f),
    b("roary", "Roary", "the Lion", "🦁", 0xFFFFE082, 0xFFFF8F00, 0.7f),
    b("dax", "Dax", "the Dino", "🦖", 0xFFA5D6A7, 0xFF2E7D32, 0.6f),
    b("hoot", "Hoot", "the Owl", "🦉", 0xFFD7CCC8, 0xFF6D4C41, 0.85f),
    b("pinky", "Pinky", "the Piggy", "🐷", 0xFFF8BBD0, 0xFFEC407A, 1.0f),
    b("zara", "Zara", "the Fox", "🦊", 0xFFFFAB91, 0xFFE64A19, 1.3f),
    b("rafi", "Rafi", "the Robot", "🤖", 0xFF80DEEA, 0xFF455A64, 0.7f),
    b("zog", "Zog", "the Alien", "👽", 0xFFD1C4E9, 0xFF673AB7, 1.8f),
    b("milo", "Milo", "the Mouse", "🐭", 0xFFCFD8DC, 0xFF78909C, 1.8f),
    b("fred", "Fred", "the Frog", "🐸", 0xFFC5E1A5, 0xFF2E7D32, 1.1f),
    b("penny", "Penny", "the Penguin", "🐧", 0xFF90CAF9, 0xFF283593, 1.2f),
    b("uni", "Uni", "the Unicorn", "🦄", 0xFFF8BBD0, 0xFF7E57C2, 1.5f),
    b("monty", "Monty", "the Monster", "👾", 0xFFB39DDB, 0xFF4527A0, 0.6f),
)

fun buddyById(id: String?): Buddy = BUDDIES.firstOrNull { it.id == id } ?: BUDDIES[0]

/** Data belongs to the child's NAME/profile, never to the character. */
data class Profile(val id: String, val name: String, val level: Int, val grade: String, val defaultBuddy: String)

val PROFILES = listOf(
    Profile("aliha", "Aliha", 3, "KG-II · 7 years", "bella"),
    Profile("affan", "M Affan", 2, "KG-I · 5 years", "dax"),
    Profile("aenan", "Aenan", 1, "Little Learner · 3 years", "teddy"),
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
    fun buddyId(profile: Profile): String {
        val v = prefs.getString("buddy_${profile.id}", null)
        return if (v != null && BUDDIES.any { it.id == v }) v else profile.defaultBuddy
    }
    fun setBuddy(pid: String, buddyId: String) = prefs.edit().putString("buddy_$pid", buddyId).apply()
    fun progress(pid: String) = Progress.fromJson(prefs.getString("prog_$pid", null))
    fun saveProgress(pid: String, p: Progress) = prefs.edit().putString("prog_$pid", p.toJson()).apply()
    fun resetProgress(pid: String) = prefs.edit().remove("prog_$pid").apply()
    /** Counts consecutive days the child opened the app. */
    fun touchStreak(pid: String): Int {
        val today = System.currentTimeMillis() / 86_400_000L
        val last = prefs.getLong("last_$pid", -1L)
        val cur = prefs.getInt("streak_$pid", 0)
        val n = when { last == today -> cur; last == today - 1 -> cur + 1; else -> 1 }
        prefs.edit().putLong("last_$pid", today).putInt("streak_$pid", n).apply()
        return n
    }
    fun pin(): String = prefs.getString("pin", "1234") ?: "1234"
    fun setPin(p: String) = prefs.edit().putString("pin", p).apply()
}
