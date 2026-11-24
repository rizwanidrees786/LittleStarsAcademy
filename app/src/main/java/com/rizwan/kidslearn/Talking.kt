package com.rizwan.kidslearn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.random.Random

// =============================================================== Character definitions (drawn 100% by code)
enum class Species { CAT, DOG, BUNNY, BEAR, PANDA, PARROT, SLIME, LION, DINO, OWL, PIG, FOX, ROBOT, ALIEN, MOUSE, FROG, PENGUIN, UNICORN, MONSTER }

data class Spec(val species: Species, val body: Color, val shade: Color, val belly: Color, val accent: Color, val extra: Color = accent)

private fun mk(s: Species, body: Long, shade: Long, belly: Long, accent: Long, extra: Long = accent) =
    Spec(s, Color(body), Color(shade), Color(belly), Color(accent), Color(extra))

val SPECS: Map<String, Spec> = mapOf(
    "mimo" to mk(Species.CAT, 0xFFFF9F43, 0xFFD9661F, 0xFFFFE3C2, 0xFFFF8FAB),
    "bruno" to mk(Species.DOG, 0xFFC68642, 0xFF8D5524, 0xFFF5DEB3, 0xFF6F4E37, 0xFF3B2A1A),
    "bella" to mk(Species.BUNNY, 0xFFFFD6E8, 0xFFE99ABF, 0xFFFFFFFF, 0xFFFF8FB8),
    "teddy" to mk(Species.BEAR, 0xFFB5835A, 0xFF7A5230, 0xFFF1D9B5, 0xFF8B5E3C, 0xFF3B2A1A),
    "pogo" to mk(Species.PANDA, 0xFFFFFFFF, 0xFFC9CED6, 0xFFFFFFFF, 0xFF2B2B36, 0xFF2B2B36),
    "kiwi" to mk(Species.PARROT, 0xFF37D67A, 0xFF1B9B52, 0xFFB6F27F, 0xFFFFA726, 0xFFFF5252),
    "gloop" to mk(Species.SLIME, 0xFF7CE35B, 0xFF39A92B, 0xFFE8FFD0, 0xFFFF8FB8),
    "blu" to mk(Species.SLIME, 0xFF5CC8FF, 0xFF2A8BD6, 0xFFE0F4FF, 0xFFFFB3D9),
    "roary" to mk(Species.LION, 0xFFFFC04D, 0xFFD9902B, 0xFFFFE9B8, 0xFFB85C1E, 0xFF5A2E12),
    "dax" to mk(Species.DINO, 0xFF6FD66F, 0xFF2E9B3E, 0xFFE8F7A8, 0xFFFF7043),
    "hoot" to mk(Species.OWL, 0xFF9C7A5B, 0xFF5E442C, 0xFFF2E1C7, 0xFFFFB300, 0xFFFFE082),
    "pinky" to mk(Species.PIG, 0xFFFFA6C9, 0xFFE0709F, 0xFFFFD0E3, 0xFFFF7FB0),
    "zara" to mk(Species.FOX, 0xFFFF7A3D, 0xFFCC4A12, 0xFFFFFFFF, 0xFF2B1A12),
    "rafi" to mk(Species.ROBOT, 0xFFB0BEC5, 0xFF607D8B, 0xFFECEFF1, 0xFF00E5FF),
    "zog" to mk(Species.ALIEN, 0xFFB388FF, 0xFF7E57C2, 0xFFE1D5FF, 0xFF76FF03),
    "milo" to mk(Species.MOUSE, 0xFFB0B7C3, 0xFF7C8594, 0xFFF1F3F8, 0xFFFF9EB5),
    "fred" to mk(Species.FROG, 0xFF4CD964, 0xFF1F9B3A, 0xFFD9F7A8, 0xFFFF6F91),
    "penny" to mk(Species.PENGUIN, 0xFF2F3A52, 0xFF161C2B, 0xFFFFFFFF, 0xFFFFA000),
    "uni" to mk(Species.UNICORN, 0xFFFFFFFF, 0xFFD6D9F0, 0xFFFFFFFF, 0xFFFF8FD2, 0xFFFFD54F),
    "monty" to mk(Species.MONSTER, 0xFF8E5CF5, 0xFF5B34B8, 0xFFD9C7FF, 0xFFFFD54F),
)

/** Everything that can move: the pose is rebuilt every frame by the Talk screen. */
class Pose(
    val breath: Float = 0f, val blink: Float = 0f, val mouth: Float = 0f, val look: Offset = Offset.Zero,
    val tilt: Float = 0f, val squash: Float = 0f, val shakeX: Float = 0f, val hop: Float = 0f, val lean: Float = 0f,
    val armR: Float = -12f, val sleeping: Boolean = false, val surprised: Boolean = false
)

// =============================================================== Drawing helpers
private fun Color.lighten(f: Float) = Color(red + (1f - red) * f, green + (1f - green) * f, blue + (1f - blue) * f, alpha)
private fun Color.darken(f: Float) = Color(red * (1f - f), green * (1f - f), blue * (1f - f), alpha)

/** An oval with a soft 3D look: light from the top-left, a dark edge and a glossy highlight. */
private fun DrawScope.sphere(c: Offset, rx: Float, ry: Float, base: Color, shade: Color) {
    val brush = Brush.radialGradient(
        listOf(base.lighten(0.35f), base, shade),
        center = Offset(c.x - rx * 0.35f, c.y - ry * 0.4f),
        radius = maxOf(rx, ry) * 1.7f
    )
    drawOval(brush, Offset(c.x - rx, c.y - ry), Size(rx * 2f, ry * 2f))
    drawOval(shade.darken(0.5f).copy(alpha = 0.55f), Offset(c.x - rx, c.y - ry), Size(rx * 2f, ry * 2f), style = Stroke(width = 0.9f))
    drawOval(Color.White.copy(alpha = 0.20f), Offset(c.x - rx * 0.55f, c.y - ry * 0.82f), Size(rx * 0.75f, ry * 0.36f))
}

private fun DrawScope.tri(a: Offset, b: Offset, c: Offset, color: Color) {
    val path = Path()
    path.moveTo(a.x, a.y); path.lineTo(b.x, b.y); path.lineTo(c.x, c.y); path.close()
    drawPath(path, color)
}

private fun DrawScope.eye(c: Offset, r: Float, p: Pose, bigBlack: Boolean = false) {
    if (p.sleeping) {
        drawArc(
            color = Color(0xFF2B2B3A), startAngle = 10f, sweepAngle = 160f, useCenter = false,
            topLeft = Offset(c.x - r * 0.8f, c.y - r * 0.5f), size = Size(r * 1.6f, r),
            style = Stroke(width = 1.8f, cap = StrokeCap.Round)
        )
        return
    }
    val open = (1f - 0.93f * p.blink).coerceIn(0.07f, 1f)
    if (bigBlack) {
        drawOval(Color(0xFF14101F), Offset(c.x - r, c.y - r * 1.2f * open), Size(r * 2f, r * 2.4f * open))
        if (open > 0.5f) drawOval(Color.White.copy(alpha = 0.85f), Offset(c.x - r * 0.5f, c.y - r * 0.8f), Size(r * 0.5f, r * 0.6f))
        return
    }
    drawOval(Brush.radialGradient(listOf(Color.White, Color(0xFFDDE1EE)), center = c, radius = r), Offset(c.x - r, c.y - r * open), Size(r * 2f, r * 2f * open))
    val pr = r * (if (p.surprised) 0.62f else 0.52f)
    val px = c.x + p.look.x * r * 0.38f
    val py = c.y + p.look.y * r * 0.32f
    drawOval(Color(0xFF1B1B2F), Offset(px - pr, py - pr * open), Size(pr * 2f, pr * 2f * open))
    if (open > 0.5f) drawCircle(Color.White, r * 0.17f, Offset(px - pr * 0.35f, py - pr * 0.4f))
}

private fun DrawScope.mouth(cx: Float, cy: Float, w: Float, m: Float, tongue: Color) {
    if (m < 0.08f) {
        drawArc(
            color = Color(0xFF3B2323), startAngle = 15f, sweepAngle = 150f, useCenter = false,
            topLeft = Offset(cx - w / 2f, cy - w * 0.35f), size = Size(w, w * 0.7f),
            style = Stroke(width = 1.7f, cap = StrokeCap.Round)
        )
    } else {
        val h = 2f + m * 11f
        drawOval(Color(0xFF4A1620), Offset(cx - w * 0.42f, cy), Size(w * 0.84f, h))
        drawOval(tongue, Offset(cx - w * 0.26f, cy + h * 0.5f), Size(w * 0.52f, h * 0.5f))
    }
}

private fun DrawScope.beak(color: Color, m: Float, cx: Float, cy: Float, w: Float) {
    drawOval(color.darken(0.15f), Offset(cx - w * 0.4f, cy + 2f + m * 6f), Size(w * 0.8f, 6f))
    if (m > 0.1f) drawOval(Color(0xFF4A1620), Offset(cx - w * 0.32f, cy + 2f), Size(w * 0.64f, m * 7f + 1f))
    tri(Offset(cx - w * 0.5f, cy - 3f), Offset(cx + w * 0.5f, cy - 3f), Offset(cx, cy + 9f), color)
}

// =============================================================== The characters
private fun DrawScope.head(spec: Spec, p: Pose) {
    val s = spec.species
    val b = spec.body; val sh = spec.shade; val bel = spec.belly; val ac = spec.accent; val ex = spec.extra

    // ---- parts behind the head
    when (s) {
        Species.LION -> {
            sphere(Offset(50f, 50f), 41f, 40f, ac, ac.darken(0.4f))
            sphere(Offset(26f, 24f), 9f, 9f, b, sh); sphere(Offset(74f, 24f), 9f, 9f, b, sh)
        }
        Species.CAT, Species.FOX -> {
            val tall = if (s == Species.FOX) 3f else 5f
            tri(Offset(24f, 34f), Offset(20f, tall), Offset(46f, 22f), b)
            tri(Offset(76f, 34f), Offset(80f, tall), Offset(54f, 22f), b)
            tri(Offset(28f, 31f), Offset(25f, tall + 10f), Offset(41f, 24f), if (s == Species.FOX) ex else ac)
            tri(Offset(72f, 31f), Offset(75f, tall + 10f), Offset(59f, 24f), if (s == Species.FOX) ex else ac)
        }
        Species.OWL -> {
            tri(Offset(26f, 28f), Offset(23f, 8f), Offset(43f, 22f), sh)
            tri(Offset(74f, 28f), Offset(77f, 8f), Offset(57f, 22f), sh)
        }
        Species.DINO -> {
            tri(Offset(36f, 24f), Offset(40f, 8f), Offset(45f, 21f), ac)
            tri(Offset(45f, 22f), Offset(50f, 3f), Offset(55f, 22f), ac)
            tri(Offset(55f, 21f), Offset(60f, 8f), Offset(64f, 24f), ac)
        }
        Species.BUNNY -> {
            rotate(-8f, Offset(38f, 30f)) { sphere(Offset(38f, 12f), 8f, 21f, b, sh); sphere(Offset(38f, 14f), 4.2f, 15f, ac, ac.darken(0.1f)) }
            rotate(8f, Offset(62f, 30f)) { sphere(Offset(62f, 12f), 8f, 21f, b, sh); sphere(Offset(62f, 14f), 4.2f, 15f, ac, ac.darken(0.1f)) }
        }
        Species.DOG -> {
            rotate(15f, Offset(21f, 30f)) { sphere(Offset(20f, 44f), 9f, 18f, ac, ac.darken(0.3f)) }
            rotate(-15f, Offset(79f, 30f)) { sphere(Offset(80f, 44f), 9f, 18f, ac, ac.darken(0.3f)) }
        }
        Species.BEAR -> {
            sphere(Offset(25f, 27f), 10f, 10f, b, sh); sphere(Offset(75f, 27f), 10f, 10f, b, sh)
            sphere(Offset(25f, 28f), 5f, 5f, bel, bel.darken(0.1f)); sphere(Offset(75f, 28f), 5f, 5f, bel, bel.darken(0.1f))
        }
        Species.PANDA -> { sphere(Offset(25f, 27f), 10f, 10f, ex, ex.darken(0.3f)); sphere(Offset(75f, 27f), 10f, 10f, ex, ex.darken(0.3f)) }
        Species.MOUSE -> {
            sphere(Offset(22f, 26f), 13f, 13f, b, sh); sphere(Offset(78f, 26f), 13f, 13f, b, sh)
            sphere(Offset(22f, 27f), 8f, 8f, ac, ac.darken(0.15f)); sphere(Offset(78f, 27f), 8f, 8f, ac, ac.darken(0.15f))
        }
        Species.MONSTER -> {
            tri(Offset(30f, 26f), Offset(26f, 4f), Offset(41f, 22f), ac)
            tri(Offset(70f, 26f), Offset(74f, 4f), Offset(59f, 22f), ac)
            sphere(Offset(20f, 44f), 7f, 9f, b, sh); sphere(Offset(80f, 44f), 7f, 9f, b, sh)
        }
        Species.PIG -> {
            tri(Offset(27f, 32f), Offset(23f, 12f), Offset(43f, 23f), sh)
            tri(Offset(73f, 32f), Offset(77f, 12f), Offset(57f, 23f), sh)
        }
        Species.UNICORN -> {
            tri(Offset(30f, 30f), Offset(28f, 12f), Offset(42f, 23f), b)
            tri(Offset(70f, 30f), Offset(72f, 12f), Offset(58f, 23f), b)
            sphere(Offset(21f, 36f), 8f, 11f, ac, ac.darken(0.2f)); sphere(Offset(18f, 54f), 8f, 11f, ex, ex.darken(0.2f)); sphere(Offset(22f, 70f), 7f, 10f, ac, ac.darken(0.2f))
        }
        Species.PARROT -> {
            rotate(-22f, Offset(50f, 30f)) { sphere(Offset(50f, 14f), 4.5f, 13f, ex, ex.darken(0.25f)) }
            sphere(Offset(50f, 13f), 4.5f, 14f, ac, ac.darken(0.25f))
            rotate(22f, Offset(50f, 30f)) { sphere(Offset(50f, 14f), 4.5f, 13f, ex, ex.darken(0.25f)) }
        }
        Species.ALIEN -> {
            drawLine(sh, Offset(38f, 26f), Offset(28f, 6f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(sh, Offset(62f, 26f), Offset(72f, 6f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            sphere(Offset(28f, 6f), 4f, 4f, ac, ac.darken(0.3f)); sphere(Offset(72f, 6f), 4f, 4f, ac, ac.darken(0.3f))
        }
        Species.FROG -> { sphere(Offset(32f, 30f), 13f, 13f, b, sh); sphere(Offset(68f, 30f), 13f, 13f, b, sh) }
        else -> {}
    }

    // ---- head
    if (s == Species.FROG) sphere(Offset(50f, 54f), 34f, 26f, b, sh) else sphere(Offset(50f, 50f), 31f, 29f, b, sh)

    // ---- face patches
    when (s) {
        Species.PANDA -> {
            rotate(-20f, Offset(35f, 47f)) { drawOval(ex, Offset(27f, 37f), Size(16f, 21f)) }
            rotate(20f, Offset(65f, 47f)) { drawOval(ex, Offset(57f, 37f), Size(16f, 21f)) }
        }
        Species.PENGUIN -> drawOval(bel, Offset(27f, 34f), Size(46f, 42f))
        Species.OWL -> {
            sphere(Offset(50f, 50f), 25f, 23f, bel, bel.darken(0.1f))
            drawCircle(ex, 13.5f, Offset(36f, 46f)); drawCircle(ex, 13.5f, Offset(64f, 46f))
        }
        Species.LION, Species.DOG, Species.BEAR, Species.FOX, Species.MOUSE, Species.DINO ->
            sphere(Offset(50f, 63f), 14f, 10.5f, bel, bel.darken(0.1f))
        Species.CAT, Species.BUNNY, Species.UNICORN -> sphere(Offset(50f, 63f), 11f, 8.5f, bel, bel.darken(0.08f))
        else -> {}
    }

    // ---- eyes
    when (s) {
        Species.PANDA -> { eye(Offset(35f, 47f), 7f, p); eye(Offset(65f, 47f), 7f, p) }
        Species.OWL -> { eye(Offset(36f, 46f), 10.5f, p); eye(Offset(64f, 46f), 10.5f, p) }
        Species.FROG -> { eye(Offset(32f, 30f), 10f, p); eye(Offset(68f, 30f), 10f, p) }
        Species.PENGUIN -> { eye(Offset(40f, 46f), 7.5f, p); eye(Offset(60f, 46f), 7.5f, p) }
        Species.ALIEN -> { eye(Offset(34f, 50f), 11f, p, true); eye(Offset(66f, 50f), 11f, p, true) }
        Species.MOUSE, Species.LION -> { eye(Offset(38f, 46f), 8f, p); eye(Offset(62f, 46f), 8f, p) }
        else -> { eye(Offset(38f, 47f), 9.5f, p); eye(Offset(62f, 47f), 9.5f, p) }
    }

    // ---- nose / beak / snout and mouth
    val m = p.mouth
    when (s) {
        Species.PARROT -> beak(ac, m, 50f, 58f, 20f)
        Species.OWL -> beak(ac, m, 50f, 58f, 12f)
        Species.PENGUIN -> beak(ac, m, 50f, 56f, 16f)
        Species.PIG -> {
            sphere(Offset(50f, 62f), 13f, 9.5f, ac, ac.darken(0.15f))
            drawOval(Color(0x99000000), Offset(44.5f, 59f), Size(3.5f, 6f)); drawOval(Color(0x99000000), Offset(52f, 59f), Size(3.5f, 6f))
            mouth(50f, 73f, 14f, m, Color(0xFFFF8FA8))
        }
        Species.FROG -> mouth(50f, 66f, 28f, m, Color(0xFFFF6F91))
        Species.DINO -> {
            drawCircle(Color(0x88000000), 1.3f, Offset(44f, 58f)); drawCircle(Color(0x88000000), 1.3f, Offset(56f, 58f))
            mouth(50f, 68f, 22f, m, Color(0xFFFF8FA8))
        }
        Species.ALIEN -> mouth(50f, 68f, 14f, m, Color(0xFF9C27B0))
        Species.MONSTER -> {
            mouth(50f, 64f, 22f, m, Color(0xFFFF8FA8))
            if (m < 0.08f) { tri(Offset(44f, 66f), Offset(48f, 66f), Offset(46f, 71f), Color.White); tri(Offset(52f, 66f), Offset(56f, 66f), Offset(54f, 71f), Color.White) }
        }
        else -> {
            val noseCol = when (s) { Species.CAT, Species.MOUSE, Species.BUNNY -> ac; Species.FOX, Species.PANDA, Species.DOG, Species.LION, Species.BEAR -> ex; else -> Color(0xFF3B2A1A) }
            if (s != Species.UNICORN) sphere(Offset(50f, 57f), 4.6f, 3.4f, noseCol, noseCol.darken(0.3f))
            mouth(50f, 66f, 15f, m, Color(0xFFFF8FA8))
            if (s == Species.BUNNY && m < 0.08f) {
                drawRoundRect(Color.White, Offset(46.2f, 66f), Size(3.5f, 5.5f), CornerRadius(1.2f))
                drawRoundRect(Color.White, Offset(50.3f, 66f), Size(3.5f, 5.5f), CornerRadius(1.2f))
            }
        }
    }

    // ---- cheeks and whiskers
    if (s != Species.OWL && s != Species.PANDA) {
        drawCircle(Color(0xFFFF6F91).copy(alpha = 0.32f), 5.2f, Offset(28f, 61f))
        drawCircle(Color(0xFFFF6F91).copy(alpha = 0.32f), 5.2f, Offset(72f, 61f))
    }
    if (s == Species.CAT || s == Species.MOUSE || s == Species.FOX) {
        val w = Color.White.copy(alpha = 0.85f)
        for (dy in listOf(-4f, 0f, 4f)) {
            drawLine(w, Offset(34f, 62f + dy / 2f), Offset(12f, 58f + dy * 1.6f), strokeWidth = 0.9f, cap = StrokeCap.Round)
            drawLine(w, Offset(66f, 62f + dy / 2f), Offset(88f, 58f + dy * 1.6f), strokeWidth = 0.9f, cap = StrokeCap.Round)
        }
    }
    // ---- front extras
    if (s == Species.UNICORN) tri(Offset(44f, 24f), Offset(50f, -2f), Offset(56f, 24f), ex)
}

private fun DrawScope.standard(spec: Spec, p: Pose, headOnly: Boolean) {
    val s = spec.species
    val b = spec.body; val sh = spec.shade; val bel = spec.belly; val ac = spec.accent; val ex = spec.extra
    if (!headOnly) {
        when (s) {
            Species.PARROT -> for (a in listOf(-16f, 0f, 16f)) rotate(a, Offset(50f, 108f)) { sphere(Offset(50f, 126f), 6f, 20f, if (a == 0f) ex else ac, sh) }
            Species.DINO -> sphere(Offset(82f, 112f), 16f, 7f, b, sh)
            Species.CAT, Species.FOX -> rotate(-35f, Offset(70f, 112f)) { sphere(Offset(78f, 98f), 6.5f, 20f, b, sh) }
            Species.MOUSE -> drawLine(ac, Offset(72f, 114f), Offset(92f, 100f), strokeWidth = 2.5f, cap = StrokeCap.Round)
            Species.PIG -> drawCircle(ac, 4f, Offset(78f, 108f))
            Species.UNICORN -> rotate(-20f, Offset(70f, 112f)) { sphere(Offset(78f, 100f), 7f, 18f, ac, sh) }
            else -> {}
        }
        val foot = if (s == Species.PENGUIN) ac else sh.lighten(0.15f)
        sphere(Offset(36f, 122f), 11f, 6f, foot, foot.darken(0.3f))
        sphere(Offset(64f, 122f), 11f, 6f, foot, foot.darken(0.3f))
        sphere(Offset(50f, 98f), 27f, 28f * (1f + p.breath * 0.02f), b, sh)
        sphere(Offset(50f, 102f), 16f, 19f, bel, bel.darken(0.1f))
        val armCol = if (s == Species.PANDA) ex else b
        rotate(12f, Offset(26f, 86f)) { sphere(Offset(26f, 97f), 6.5f, 13f, armCol, sh) }
        rotate(p.armR, Offset(74f, 86f)) { sphere(Offset(74f, 97f), 6.5f, 13f, armCol, sh) }
    }
    rotate(p.tilt, Offset(50f, 78f)) { head(spec, p) }
}

private fun DrawScope.slime(spec: Spec, p: Pose, headOnly: Boolean) {
    val b = spec.body; val sh = spec.shade; val ac = spec.accent
    if (!headOnly) {
        sphere(Offset(50f, 112f), 41f, 14f, b.darken(0.05f), sh)
        sphere(Offset(14f, 114f), 7f, 9f, b, sh); sphere(Offset(87f, 115f), 6f, 8f, b, sh)
    }
    sphere(Offset(50f, 78f), 37f, 36f * (1f + p.breath * 0.02f), b, sh)
    drawOval(Color.White.copy(alpha = 0.12f), Offset(30f, 88f), Size(40f, 28f))
    drawCircle(Color.White.copy(alpha = 0.28f), 3f, Offset(26f, 60f))
    drawCircle(Color.White.copy(alpha = 0.22f), 2f, Offset(74f, 98f))
    rotate(p.tilt, Offset(50f, 105f)) {
        eye(Offset(37f, 68f), 11f, p); eye(Offset(63f, 68f), 11f, p)
        drawCircle(Color(0xFFFF6F91).copy(alpha = 0.35f), 5.5f, Offset(25f, 82f))
        drawCircle(Color(0xFFFF6F91).copy(alpha = 0.35f), 5.5f, Offset(75f, 82f))
        mouth(50f, 88f, 22f, p.mouth, ac)
    }
}

private fun DrawScope.robot(spec: Spec, p: Pose, headOnly: Boolean) {
    val b = spec.body; val sh = spec.shade; val ac = spec.accent
    val dark = Color(0xFF10172A)
    if (!headOnly) {
        drawRoundRect(sh, Offset(34f, 112f), Size(13f, 12f), CornerRadius(4f))
        drawRoundRect(sh, Offset(53f, 112f), Size(13f, 12f), CornerRadius(4f))
        drawRoundRect(Brush.verticalGradient(listOf(b.lighten(0.3f), b, sh), startY = 78f, endY = 118f), Offset(27f, 78f), Size(46f, 40f), CornerRadius(10f))
        drawRoundRect(spec.belly, Offset(37f, 88f), Size(26f, 18f), CornerRadius(5f))
        drawCircle(ac, 3f, Offset(44f, 97f)); drawCircle(Color(0xFFFF5252), 3f, Offset(56f, 97f))
        drawRoundRect(sh, Offset(16f, 82f), Size(9f, 26f), CornerRadius(4f))
        rotate(p.armR, Offset(79f, 84f)) { drawRoundRect(sh, Offset(75f, 82f), Size(9f, 26f), CornerRadius(4f)) }
    }
    rotate(p.tilt, Offset(50f, 78f)) {
        drawLine(sh, Offset(50f, 24f), Offset(50f, 9f), strokeWidth = 2f)
        drawCircle(ac, 4f + p.mouth * 1.5f, Offset(50f, 7f))
        drawRoundRect(Brush.linearGradient(listOf(b.lighten(0.35f), b, sh), start = Offset(20f, 22f), end = Offset(80f, 78f)), Offset(20f, 22f), Size(60f, 56f), CornerRadius(16f))
        drawCircle(sh, 3.5f, Offset(17f, 50f)); drawCircle(sh, 3.5f, Offset(83f, 50f))
        drawRoundRect(dark, Offset(27f, 34f), Size(46f, 26f), CornerRadius(10f))
        val open = (1f - 0.9f * p.blink).coerceIn(0.1f, 1f)
        if (p.sleeping) {
            drawRoundRect(ac, Offset(32f, 47f), Size(14f, 2.5f), CornerRadius(1f)); drawRoundRect(ac, Offset(54f, 47f), Size(14f, 2.5f), CornerRadius(1f))
        } else {
            val eh = 14f * open
            drawRoundRect(ac, Offset(32f + p.look.x * 3f, 47f - eh / 2f), Size(14f, eh), CornerRadius(4f))
            drawRoundRect(ac, Offset(54f + p.look.x * 3f, 47f - eh / 2f), Size(14f, eh), CornerRadius(4f))
        }
        drawRoundRect(dark, Offset(36f, 65f), Size(28f, 5f + p.mouth * 9f), CornerRadius(3f))
        for (i in 0..4) drawLine(ac.copy(alpha = 0.6f), Offset(40f + i * 5f, 65f), Offset(40f + i * 5f, 70f + p.mouth * 9f), strokeWidth = 0.8f)
    }
}

/** Draws a whole character (or only its head, for small avatars). Works in a 100 x 130 design space. */
fun DrawScope.drawCharacter(spec: Spec, p: Pose, headOnly: Boolean = false) {
    val slimeLike = spec.species == Species.SLIME
    val rx: Float; val ry: Float; val rw: Float; val rh: Float
    if (!headOnly) { rx = 0f; ry = 0f; rw = 100f; rh = 130f }
    else if (slimeLike) { rx = 8f; ry = 40f; rw = 84f; rh = 82f }
    else if (spec.species == Species.ROBOT) { rx = 14f; ry = 0f; rw = 72f; rh = 82f }
    else { rx = 8f; ry = -2f; rw = 84f; rh = 84f }
    val sc = minOf(size.width / rw, size.height / rh)
    val tx = (size.width - rw * sc) / 2f - rx * sc
    val ty = (size.height - rh * sc) / 2f - ry * sc
    translate(tx, ty) {
        scale(sc, sc, Offset.Zero) {
            if (!headOnly) drawOval(Color.Black.copy(alpha = 0.22f), Offset(18f, 119f), Size(64f, 10f))
            translate(p.shakeX, -p.hop) {
                rotate(p.lean, Offset(50f, 124f)) {
                    scale(1f + p.squash * 0.5f, 1f - p.squash, Offset(50f, 124f)) {
                        when (spec.species) {
                            Species.SLIME -> slime(spec, p, headOnly)
                            Species.ROBOT -> robot(spec, p, headOnly)
                            else -> standard(spec, p, headOnly)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterHead(id: String, modifier: Modifier = Modifier) {
    val spec = SPECS[id] ?: return
    Canvas(modifier) { drawCharacter(spec, Pose(), headOnly = true) }
}

// =============================================================== Talking screen (touch, talk, real voice echo, feed, dance, sing...)
enum class Mood { IDLE, SLEEP, EAT, DRINK, DANCE, WAVE }
private class Particle(val e: String, val x: Float, val y: Float, val t0: Float)

private val SONGS = listOf(
    "Twinkle, twinkle, little star. How I wonder what you are! Up above the world so high. Like a diamond in the sky.",
    "Row, row, row your boat. Gently down the stream. Merrily, merrily, merrily, merrily. Life is but a dream.",
    "Baa, baa, black sheep, have you any wool? Yes sir, yes sir, three bags full.",
    "Old MacDonald had a farm. E I E I O. And on his farm he had a cow. E I E I O.",
    "The itsy bitsy spider climbed up the water spout. Down came the rain and washed the spider out.",
    "Mary had a little lamb. Its fleece was white as snow. And everywhere that Mary went. The lamb was sure to go."
)

@Composable
private fun RoundBtn(emoji: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 28.sp) }
}

@Composable
fun TalkScreen(s: AppState, onBack: () -> Unit) {
    val profile = s.profile ?: return
    val buddy = s.buddy
    val spec = SPECS[buddy.id] ?: SPECS.getValue("mimo")
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val echoEng = remember { EchoEngine() }

    var time by remember { mutableFloatStateOf(0f) }
    var look by remember { mutableStateOf(Offset.Zero) }
    var lookAt by remember { mutableFloatStateOf(-10f) }
    var react by remember { mutableIntStateOf(0) }
    var reactT by remember { mutableFloatStateOf(-10f) }
    var mood by remember { mutableStateOf(Mood.IDLE) }
    var moodT by remember { mutableFloatStateOf(-10f) }
    var echo by remember { mutableStateOf(false) }
    var urdu by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var youSaid by remember { mutableStateOf("") }
    var botSaid by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var food by remember { mutableStateOf("🍎") }
    var cs by remember { mutableStateOf(IntSize(1, 1)) }
    var petDist by remember { mutableFloatStateOf(0f) }
    var shakeFlag by remember { mutableIntStateOf(0) }
    val particles = remember { mutableStateListOf<Particle>() }
    val hist = remember { mutableStateListOf<ChatMsg>() }

    fun hasMic() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) echo = true else status = "Please allow the microphone (ask Mama or Papa)."
    }

    LaunchedEffect(Unit) { while (true) { withFrameNanos { time = it / 1e9f } } }
    LaunchedEffect(particles.size) { delay(1600); particles.removeAll { time - it.t0 > 1.4f } }
    LaunchedEffect(moodT) {
        val d = when (mood) { Mood.EAT, Mood.DRINK -> 2700L; Mood.DANCE -> 5000L; Mood.WAVE -> 2700L; else -> 0L }
        if (d > 0L) { delay(d); if (mood != Mood.SLEEP) mood = Mood.IDLE }
    }
    // shake the phone -> the pet gets dizzy
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                if (g > 24f) {
                    val now = System.currentTimeMillis()
                    if (now - last > 3000L) { last = now; shakeFlag++ }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l); s.speaker.stop() }
    }

    fun say(text: String) { botSaid = text; s.say(text) }
    fun doReact(kind: Int) { react = kind; reactT = time }
    fun spawn(e: String, pos: Offset) { particles.add(Particle(e, pos.x, pos.y, time)) }
    fun lookToward(pos: Offset, w: Int, h: Int) {
        look = Offset(((pos.x / w) - 0.5f) * 2f, ((pos.y / h) - 0.4f) * 2f)
        lookAt = time
    }
    fun zoneOf(pos: Offset, w: Int, h: Int): Int {
        val sc = minOf(w / 100f, h / 130f)
        val ox = (w - 100f * sc) / 2f
        val oy = (h - 130f * sc) / 2f
        val dx = (pos.x - ox) / sc
        val dy = (pos.y - oy) / sc
        return when {
            dy < 40f && (dx < 33f || dx > 67f) -> 4
            dy in 38f..56f && dx in 28f..72f -> 5
            dy in 56f..74f && dx in 36f..64f -> 6
            dy < 80f -> 0
            dy < 118f -> 1
            else -> 2
        }
    }
    fun setMood(m: Mood) { mood = m; moodT = time }

    fun handleTap(pos: Offset, w: Int, h: Int) {
        if (mood == Mood.SLEEP) { setMood(Mood.IDLE); say("Oh! Good morning, ${profile.name}!"); return }
        lookToward(pos, w, h)
        when (zoneOf(pos, w, h)) {
            4 -> { doReact(4); say(listOf("Hey! My ears!", "Hehe, that tickles my ear!", "Wiggle wiggle!").random()) }
            5 -> { doReact(5); say(listOf("Ouch! My eyes!", "Hey! I can't see!", "Don't poke my eyes!").random()) }
            6 -> { doReact(6); say(listOf("Boop!", "Hehe, my nose!", "Beep beep!").random()) }
            0 -> { doReact(1); say(listOf("Ouch!", "Hey! That is my head!", "Ow! Be gentle!").random()) }
            1 -> { doReact(2); spawn("💗", pos); say(listOf("Hehehe! That tickles!", "Hahaha! Stop it!", "Hee hee hee!").random()) }
            else -> { doReact(3); say(listOf("Whee!", "Jump jump jump!", "Hee hee, my feet!").random()) }
        }
    }

    fun handlePet(pos: Offset, dist: Float, w: Int, h: Int) {
        if (mood == Mood.SLEEP) return
        lookToward(pos, w, h)
        petDist += dist
        if (petDist > 230f) {
            petDist = 0f
            doReact(2); spawn("💕", pos)
            if (Random.nextInt(3) == 0) say(listOf("Hehe, that feels nice!", "I love it!", "So ticklish!", "Aww, thank you!").random())
        }
    }

    fun act(kind: Mood) {
        setMood(kind)
        when (kind) {
            Mood.EAT -> { food = listOf("🍎", "🍌", "🍪", "🍕", "🍓", "🍉").random(); say("Yummy! Crunch crunch! Delicious!") }
            Mood.DRINK -> { food = "🥛"; say("Glug glug glug! So tasty!") }
            Mood.SLEEP -> say("Good night ${profile.name}. Sweet dreams. Zzz.")
            Mood.DANCE -> say("La la la! Let's dance, ${profile.name}!")
            Mood.WAVE -> say("Hello ${profile.name}! I am ${buddy.name}. How are you today?")
            else -> {}
        }
    }

    fun handleSpeech(t: String) {
        youSaid = t
        if (Safety.isUnsafe(t)) { say(Safety.redirect(profile.name)); return }
        if (echo) {
            botSaid = t
            if (urdu && s.speaker.urduAvailable) s.speaker.speakUr(t)
            else s.speaker.speakEn(t, (buddy.pitch * 1.35f).coerceIn(0.9f, 2.0f), 1.1f)
            doReact(2)
        } else {
            botSaid = "…"
            scope.launch {
                val r = s.brain.reply(profile, buddy, hist.toList(), t)
                hist.add(ChatMsg(true, t)); hist.add(ChatMsg(false, r))
                say(r)
                reward(s, "Speaking", "Chat", true)
            }
        }
    }

    val toggleMic = rememberMic(s, onResult = { handleSpeech(it) }, onStatus = { status = it })

    LaunchedEffect(echo) {
        if (echo) {
            s.speaker.stop(); s.voice.stop()
            echoEng.run({ (0.9f + s.buddy.pitch * 0.45f).coerceIn(1.25f, 1.9f) }, { echo }, { s.speaker.speaking })
        }
    }
    LaunchedEffect(shakeFlag) { if (shakeFlag > 0) { doReact(7); say("Whoa! I am so dizzy!") } }
    LaunchedEffect(buddy.id) {
        s.ensureModelLoaded()
        delay(250)
        setMood(Mood.WAVE)
        say("Hi ${profile.name}! Touch me, poke me, or talk to me!")
    }

    // ------------------------------------------------ build the pose for this frame
    val speaking = s.speaker.speaking
    val hearing = s.voice.listening || echoEng.state == 2
    val ph = time % 4.2f
    var blink = if (ph > 4.0f) { val k = (ph - 4.0f) / 0.2f; if (k < 0.5f) k * 2f else (1f - k) * 2f } else 0f
    val re = (time - reactT).coerceAtLeast(0f)
    val amp = (1f - re / 0.7f).coerceIn(0f, 1f)
    val ampL = (1f - re / 2.2f).coerceIn(0f, 1f)
    val mt = (time - moodT).coerceAtLeast(0f)
    var mouth = if (speaking) 0.25f + 0.75f * abs(sin(time * 13f)) else 0f
    if (echoEng.state == 3) mouth = maxOf(mouth, (0.12f + echoEng.level * 1.4f).coerceAtMost(1f))
    var squash = 0f; var shake = 0f; var hop = 0f; var lean = 0f; var armR = -12f
    var tilt = sin(time * 0.9f) * 1.5f
    var lk = if (time - lookAt < 2.2f) look else Offset.Zero
    when (react) {
        1 -> { mouth = maxOf(mouth, 0.85f * amp); squash = 0.12f * amp * abs(sin(re * 30f)); shake = sin(re * 50f) * 2.5f * amp }
        2 -> { mouth = maxOf(mouth, 0.7f * amp); shake = sin(re * 40f) * 1.5f * amp; hop = 2f * amp * abs(sin(re * 20f)) }
        3 -> { hop = 12f * amp * sin(re / 0.7f * 3.14159f).coerceAtLeast(0f); mouth = maxOf(mouth, 0.5f * amp) }
        4 -> { tilt = sin(re * 35f) * 9f * amp; mouth = maxOf(mouth, 0.35f * amp) }
        5 -> { blink = maxOf(blink, amp); mouth = maxOf(mouth, 0.8f * amp); shake = sin(re * 45f) * 1.5f * amp }
        6 -> { squash = 0.09f * amp * abs(sin(re * 25f)); lk = Offset(0f, -0.6f * amp); mouth = maxOf(mouth, 0.3f * amp) }
        7 -> { tilt = sin(time * 11f) * 14f * ampL; lean = sin(time * 6f) * 5f * ampL; lk = Offset(cos(time * 9f), sin(time * 9f)) * ampL; mouth = maxOf(mouth, 0.35f * ampL) }
    }
    when (mood) {
        Mood.DANCE -> { lean = sin(time * 7f) * 7f; hop += abs(sin(time * 7f)) * 5f; tilt = sin(time * 7f) * 6f; mouth = maxOf(mouth, 0.45f + 0.3f * sin(time * 7f)) }
        Mood.WAVE -> armR = -150f + sin(time * 10f) * 18f
        Mood.EAT, Mood.DRINK -> { val k = mt / 0.9f; mouth = if (k < 1f) (if (k > 0.5f) 0.9f else 0f) else abs(sin(time * 10f)) * 0.55f }
        else -> {}
    }
    // little life: looks around and yawns when nobody is playing
    val quiet = mood == Mood.IDLE && !speaking && !hearing && amp < 0.02f && ampL < 0.02f && time - lookAt > 2.2f && echoEng.state != 3
    if (quiet) {
        val c = time % 14f
        if (c in 3f..6f) lk = Offset(sin((c - 3f) * 1.05f) * 0.9f, 0.1f)
        if (c in 9f..10.6f) { val y = sin((c - 9f) / 1.6f * 3.14159f); mouth = maxOf(mouth, 0.85f * y); blink = maxOf(blink, 0.6f * y) }
    }
    if (hearing) tilt = 7f * sin(time * 3f)
    val pose = Pose(
        breath = sin(time * 2.2f), blink = blink, mouth = mouth, look = lk,
        tilt = tilt, squash = squash, shakeX = shake, hop = hop, lean = lean, armR = armR,
        sleeping = mood == Mood.SLEEP, surprised = hearing || (react == 1 && amp > 0.1f)
    )
    val echoLine = when (echoEng.state) { 2 -> "🎙 I hear you!"; 3 -> "🦜 Repeating!"; else -> "👂 Talk to me, I will repeat everything!" }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(top = 50.dp, bottom = 150.dp).onSizeChanged { cs = it }) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { pos -> handleTap(pos, size.width, size.height) }) }
                    .pointerInput(Unit) { detectDragGestures(onDrag = { change, delta -> change.consume(); handlePet(change.position, delta.getDistance(), size.width, size.height) }) }
            ) {
                drawCircle(
                    Brush.radialGradient(listOf(spec.body.copy(alpha = 0.38f), Color.Transparent), center = Offset(size.width / 2f, size.height * 0.5f), radius = size.width * 0.8f),
                    radius = size.width * 0.8f, center = Offset(size.width / 2f, size.height * 0.5f)
                )
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.14f)), startY = size.height * 0.75f, endY = size.height),
                    topLeft = Offset(0f, size.height * 0.75f), size = Size(size.width, size.height * 0.25f)
                )
                drawCharacter(spec, pose)
            }
            particles.forEach { pt ->
                val age = time - pt.t0
                if (age in 0f..1.4f) Text(
                    pt.e, fontSize = 34.sp,
                    modifier = Modifier.offset { IntOffset(pt.x.toInt() - 40, (pt.y - age * 170f).toInt() - 40) }.alpha(1f - age / 1.4f)
                )
            }
            if ((mood == Mood.EAT || mood == Mood.DRINK) && mt < 0.9f) {
                val k = mt / 0.9f
                val sc = minOf(cs.width / 100f, cs.height / 130f)
                val mouthY = (cs.height - 130f * sc) / 2f + 64f * sc
                val y = cs.height + (mouthY - cs.height) * k
                Text(food, fontSize = 50.sp, modifier = Modifier.offset { IntOffset(cs.width / 2 - 60, y.toInt() - 60) })
            }
            if (mood == Mood.SLEEP) {
                val zk = (time % 2f) / 2f
                Text("💤", fontSize = 40.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = (50f - zk * 40f).dp).alpha(1f - zk * 0.6f))
            }
            if (react == 7 && ampL > 0.05f) Text("💫", fontSize = 44.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        }

        if (botSaid.isNotEmpty()) GlassCard(Modifier.align(Alignment.TopCenter).padding(top = 46.dp, start = 70.dp, end = 70.dp)) {
            Text(botSaid, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 4)
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { s.voice.stop(); s.speaker.stop(); echo = false; onBack() }, contentAlignment = Alignment.Center) {
                Text("←", color = Color.White, fontSize = 22.sp)
            }
            Spacer(Modifier.weight(1f))
            Chip("🔄 ${buddy.name}", true, Modifier.width(150.dp)) { showPicker = true }
        }

        Column(Modifier.align(Alignment.CenterStart).padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBtn("🍎") { act(Mood.EAT) }
            RoundBtn("🥛") { act(Mood.DRINK) }
            RoundBtn("😴") { if (mood == Mood.SLEEP) setMood(Mood.IDLE) else act(Mood.SLEEP) }
            RoundBtn("🎵") { act(Mood.DANCE); say(SONGS.random()) }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBtn("💃") { act(Mood.DANCE) }
            RoundBtn("👋") { act(Mood.WAVE) }
            RoundBtn("😂") { doReact(2); say(Content.jokes.random()) }
            RoundBtn("📖") { val st = Content.stories.random(); say(st.title + ". " + st.lines.joinToString(" ") { it.en }) }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (youSaid.isNotEmpty() && !echo) Text("You: $youSaid", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 1)
            if (echo) Text(echoLine, color = SunYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (status.isNotEmpty()) Text(status, color = SunYellow, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.width(4.dp))
                Chip("💬 Chat", !echo, Modifier.width(92.dp)) { echo = false; status = "" }
                Chip("🦜 Repeat me", echo, Modifier.width(120.dp)) { if (hasMic()) { status = ""; echo = true } else permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                Chip("English", !urdu, Modifier.width(84.dp)) { urdu = false }
                Chip("اردو", urdu, Modifier.width(70.dp)) { urdu = true }
                Chip("⌨ Type", typing, Modifier.width(84.dp)) { typing = !typing }
                Chip(if (s.aiOn) "🧠 AI ON" else "🧠 AI OFF", s.aiOn, Modifier.width(100.dp)) { s.toggleAi() }
                Spacer(Modifier.width(4.dp))
            }
            if (typing) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Type here…") }, singleLine = true)
                Spacer(Modifier.width(8.dp))
                GradientButton("➤") { if (input.isNotBlank()) { val t = input; input = ""; handleSpeech(t) } }
            }
            if (!echo) MicButton(hearing, size = 76.dp) { toggleMic(if (urdu) "ur-PK" else "en-US") }
        }

        if (showPicker) Box(Modifier.fillMaxSize().background(Color(0xEE0A0B2E)).clickable { showPicker = false }) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Text("Choose your friend", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BUDDIES) { bd ->
                        Column(
                            Modifier.clickable {
                                s.chooseBuddy(bd); hist.clear(); particles.clear(); showPicker = false
                                setMood(Mood.WAVE); say("Hi! My name is ${bd.name}. ${bd.name}. Can you say ${bd.name}?")
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BuddyAvatar(bd, 62.dp)
                            Text(bd.name, color = if (bd.id == buddy.id) SunYellow else Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
