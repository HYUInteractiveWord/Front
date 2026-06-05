package com.interactiveword.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactiveword.ui.theme.BrandGreenLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.random.Random

object XpManager {
    private val _xpEvents = MutableSharedFlow<Int>()
    val xpEvents = _xpEvents.asSharedFlow()

    suspend fun emitXpGain(amount: Int) {
        _xpEvents.emit(amount)
    }
}

@Composable
fun XpGainOverlay() {
    var visible by remember { mutableStateOf(false) }
    var xpAmount by remember { mutableIntStateOf(0) }
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(Unit) {
        XpManager.xpEvents.collect { amount ->
            xpAmount = amount
            visible = true
            
            // XP 양에 비례한 파티클 생성
            val count = when {
                amount >= 500 -> 60
                amount >= 150 -> 35
                else -> 15
            }
            
            particles.clear()
            repeat(count) {
                particles.add(Particle(
                    color = listOf(Color.Yellow, Color.White, Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF64FFDA)).random(),
                    size = Random.nextFloat() * 10f + 6f,
                    angle = Random.nextFloat() * 360f,
                    velocity = Random.nextFloat() * 25f + 10f,
                    shapeType = Random.nextInt(3) // 0: Square, 1: Circle, 2: Star-ish
                ))
            }
            
            delay(2500)
            visible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (visible) {
            ConfettiEffect(particles, xpAmount)
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .wrapContentSize(),
                shape = RoundedCornerShape(50.dp),
                color = BrandGreenLight,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Text("XP +$xpAmount!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

data class Particle(
    val color: Color,
    val size: Float,
    val angle: Float,
    val velocity: Float,
    val shapeType: Int
)

@Composable
private fun ConfettiEffect(particles: List<Particle>, amount: Int) {
    val duration = if (amount >= 500) 2500 else 1800
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(duration, easing = LinearOutSlowInEasing)),
        label = "progress"
    )

    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val topPx = with(density) { 140.dp.toPx() }

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val gravity = progress * progress * 800f
                val airResistance = 1f - (progress * 0.5f)
                
                val moveX = kotlin.math.cos(Math.toRadians(p.angle.toDouble())).toFloat() * p.velocity * progress * 50f * airResistance
                val moveY = kotlin.math.sin(Math.toRadians(p.angle.toDouble())).toFloat() * p.velocity * progress * 50f * airResistance + gravity
                
                val currentX = (width / 2) + moveX
                val currentY = topPx + moveY

                rotate(degrees = progress * 1080f, pivot = Offset(currentX, currentY)) {
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    when (p.shapeType) {
                        0 -> drawRect(p.color.copy(alpha = alpha), Offset(currentX, currentY), androidx.compose.ui.geometry.Size(p.size, p.size))
                        1 -> drawCircle(p.color.copy(alpha = alpha), p.size / 2, Offset(currentX, currentY))
                        else -> {
                            val s = p.size
                            drawRect(p.color.copy(alpha = alpha), Offset(currentX, currentY), androidx.compose.ui.geometry.Size(s, s / 3))
                            drawRect(p.color.copy(alpha = alpha), Offset(currentX + s/3, currentY - s/3), androidx.compose.ui.geometry.Size(s / 3, s))
                        }
                    }
                }
            }
        }
    }
}
