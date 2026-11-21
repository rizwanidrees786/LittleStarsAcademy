package com.rizwan.kidslearn

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

val Purple = Color(0xFF7C4DFF)
val Pink = Color(0xFFFF4FA3)
val Teal = Color(0xFF00E5C3)
val SunYellow = Color(0xFFFFD54F)
val Sky = Color(0xFF40C4FF)
val Good = Color(0xFF00C853)
val Bad = Color(0xFFFF5252)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Color(0xFF0A0B2E), surface = Color(0xFF14153F)), content = content)
}

/** Animated aurora + twinkling stars background. */
@Composable
fun AnimatedBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val twinkle by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "tw")
    val stars = remember { List(40) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A0B2E), Color(0xFF1B1464), Color(0xFF2B0F54))))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val c1 = Offset(w * (0.15f + 0.6f * a), h * 0.18f)
            val c2 = Offset(w * (0.85f - 0.6f * a), h * 0.75f)
            drawCircle(Brush.radialGradient(listOf(Pink.copy(alpha = 0.35f), Color.Transparent), center = c1, radius = w * 0.8f), radius = w * 0.8f, center = c1)
            drawCircle(Brush.radialGradient(listOf(Teal.copy(alpha = 0.28f), Color.Transparent), center = c2, radius = w * 0.8f), radius = w * 0.8f, center = c2)
            stars.forEach { (x, y, s) ->
                drawCircle(Color.White.copy(alpha = (twinkle * (0.4f + s * 0.6f)).coerceIn(0f, 1f)), radius = 2f + s * 3f, center = Offset(w * x, h * y))
            }
        }
        content()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    var m = modifier.clip(shape).background(Color.White.copy(alpha = 0.10f)).border(1.dp, Color.White.copy(alpha = 0.25f), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, c1: Color = Purple, c2: Color = Pink, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(c1, c2)))
            .clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
fun BuddyAvatar(b: Buddy, size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bob")
    val dy by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1100 + b.name.length * 40, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
    Box(
        modifier.size(size).offset(y = dy.dp)
            .shadow(14.dp, CircleShape, ambientColor = b.c1, spotColor = b.c1)
            .clip(CircleShape).background(Brush.linearGradient(listOf(b.c1, b.c2)))
            .border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(b.emoji, fontSize = (size.value * 0.55f).sp) }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: String = "") {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("←", color = Color.White, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing.isNotEmpty()) Text(trailing, color = SunYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) Brush.horizontalGradient(listOf(Purple, Pink)) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun Tile(emoji: String, title: String, sub: String, c1: Color, c2: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.padding(6.dp).height(132.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c1, c2))).clickable(onClick = onClick).padding(10.dp),
        Arrangement.Center, Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 38.sp)
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
        Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Confetti(trigger: Int) {
    val p = remember { Animatable(1f) }
    val parts = remember(trigger) { List(40) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextInt(5)) } }
    val colors = listOf(Pink, Teal, SunYellow, Sky, Purple)
    LaunchedEffect(trigger) {
        if (trigger > 0) { p.snapTo(0f); p.animateTo(1f, tween(1300)) }
    }
    Canvas(Modifier.fillMaxSize()) {
        if (p.value < 1f) parts.forEach { (x, s, c) ->
            drawCircle(colors[c], radius = 8f + s * 8f,
                center = Offset(size.width * x, size.height * (p.value * (0.5f + s * 0.7f))),
                alpha = (1f - p.value).coerceIn(0f, 1f))
        }
    }
}
