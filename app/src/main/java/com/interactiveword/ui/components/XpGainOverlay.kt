package com.interactiveword.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.BrandAmberLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.random.Random

enum class NotiType {
    XP, RANK_UP, SLOT_INCREASE, TICKET, NEW_WORD, MASTER_ACHIEVED, BEST_SCORE
}

data class AppNotification(
    val type: NotiType,
    val message: String = "",
    val messageRes: Int = 0,
    val messageArgs: List<Any> = emptyList(),
    val amount: Int = 0,
    val color: Color = BrandGreenLight,
    val icon: ImageVector = Icons.Default.Star
)

object XpManager {
    private val _notifications = MutableSharedFlow<AppNotification>()
    val notifications = _notifications.asSharedFlow()

    suspend fun emitXpGain(amount: Int) {
        _notifications.emit(AppNotification(NotiType.XP, amount = amount))
    }

    suspend fun emitNotification(notification: AppNotification) {
        _notifications.emit(notification)
    }
}

@Composable
fun XpGainOverlay() {
    val context = LocalContext.current
    var currentNoti by remember { mutableStateOf<AppNotification?>(null) }
    var visible by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<Particle>() }

    // 진동 서비스 가져오기
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // 알림 큐 처리를 위한 로직
    LaunchedEffect(Unit) {
        XpManager.notifications.collect { notification ->
            // 이전 알림이 있으면 대기
            while (visible) {
                delay(100)
            }
            
            currentNoti = notification
            visible = true

            // 진동 발생
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            } catch (e: Exception) {
                // 진동 실패 시 무시
            }
            
            // 타입별 파티클 생성
            val count = when (notification.type) {
                NotiType.RANK_UP, NotiType.MASTER_ACHIEVED, NotiType.BEST_SCORE -> 80
                NotiType.XP -> if (notification.amount >= 150) 40 else 15
                else -> 30
            }
            
            particles.clear()
            val colors = when(notification.type) {
                NotiType.RANK_UP, NotiType.MASTER_ACHIEVED, NotiType.BEST_SCORE -> listOf(Color(0xFFFFD700), Color.White, Color(0xFFFFA000), BrandGreenLight)
                NotiType.TICKET -> listOf(Color(0xFFE91E63), Color.White, Color(0xFFFF4081))
                NotiType.NEW_WORD -> listOf(BrandGreenLight, Color.White, Color(0xFFB9F6CA))
                else -> listOf(Color.Yellow, Color.White, Color(0xFFFFD700), Color(0xFF00E676))
            }

            repeat(count) {
                particles.add(Particle(
                    color = colors.random(),
                    size = Random.nextFloat() * 10f + 6f,
                    angle = Random.nextFloat() * 360f,
                    velocity = Random.nextFloat() * 25f + 10f,
                    shapeType = Random.nextInt(3)
                ))
            }
            
            delay(2500)
            visible = false
            delay(300) // 알림 간 간격
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (visible && currentNoti != null) {
            ConfettiEffect(particles, currentNoti?.type == NotiType.RANK_UP)
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            currentNoti?.let { noti ->
                val displayMessage = when {
                    noti.messageRes != 0 -> {
                        if (noti.messageArgs.isNotEmpty()) {
                            context.getString(noti.messageRes, *noti.messageArgs.toTypedArray())
                        } else {
                            context.getString(noti.messageRes)
                        }
                    }
                    noti.type == NotiType.XP -> "XP +${noti.amount}!"
                    else -> noti.message
                }

                Surface(
                    modifier = Modifier
                        .padding(top = 100.dp)
                        .wrapContentSize(),
                    shape = RoundedCornerShape(50.dp),
                    color = noti.color,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(noti.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        Text(displayMessage, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
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
private fun ConfettiEffect(particles: List<Particle>, isBig: Boolean) {
    val duration = if (isBig) 2500 else 1800
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
