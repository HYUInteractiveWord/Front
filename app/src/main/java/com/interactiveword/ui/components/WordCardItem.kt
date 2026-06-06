package com.interactiveword.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactiveword.data.model.WordCard
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.util.WordCardPointManager
import kotlinx.coroutines.delay

@Composable
fun WordCardItem(
    card: WordCard,
    compact: Boolean = false,
    animateProgress: Boolean = false, // 리스트 진입 시 애니메이션 여부
    animateEntrance: Boolean = false, // 새로 추가된 아이템 등장 효과
    onPlayTts: (WordCard) -> Unit = {},
    onPlayTransTts: (WordCard) -> Unit = {},
    onClick: (WordCard) -> Unit = {},
) {
    val context = LocalContext.current
    
    // 포인트 정보 계산 (bestScore 대신 wordPoint 기반으로 통일)
    val currentPoints = card.wordPoint.coerceIn(0, 100)
    val unseenIncrease = if (animateProgress) WordCardPointManager.getUnseenPointIncrease(context, card) else 0
    val startPoints = (currentPoints - unseenIncrease).coerceAtLeast(0)
    val newLevel = if (animateProgress) WordCardPointManager.checkLevelUp(context, card) else null

    // 애니메이션 상태
    val animatedPoints = remember { Animatable(startPoints.toFloat()) }
    var showLevelUp by remember { mutableStateOf(false) }

    // 등장 효과를 위한 scale 애니메이션
    val entranceScale = remember { Animatable(if (animateEntrance) 0.8f else 1f) }

    LaunchedEffect(card.id, card.wordPoint,animateProgress, animateEntrance) {
        if (animateEntrance) {
            // "통!" 튀어오르는 효과
            entranceScale.animateTo(
                1.1f, 
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            entranceScale.animateTo(1f, animationSpec = tween(200))
        }

        if (animateProgress && unseenIncrease > 0) {
            delay(300)
            animatedPoints.animateTo(
                targetValue = currentPoints.toFloat(),
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
            if (newLevel != null) {
                showLevelUp = true
                delay(2500)
                showLevelUp = false
            }
            // 확인 완료 처리
            WordCardPointManager.markAsSeen(context, card)
        } else {
            animatedPoints.snapTo(currentPoints.toFloat())
            WordCardPointManager.markAsSeen(context, card)
        }
    }

    val displayPoint = animatedPoints.value.toInt()
    val effect = wordCardEffectStyle(displayPoint)
    val containerColor = effect.containerColor ?: MaterialTheme.colorScheme.surface

    // 랭크별 발광(Pulse) 효과
    val infiniteTransition = rememberInfiniteTransition(label = "rankGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    val borderWidth = when {
        displayPoint >= 100 -> 3.dp
        displayPoint >= 85 -> 2.dp
        displayPoint >= 65 -> 1.5.dp
        else -> 1.dp
    }
    
    val borderAlpha = if (displayPoint >= 85) glowAlpha else 1f

    Box(modifier = Modifier.fillMaxWidth().scale(entranceScale.value)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(card) },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(borderWidth, effect.borderColor.copy(alpha = borderAlpha)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (displayPoint >= 85) 6.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = card.koreanWord,
                                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { onPlayTts(card) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "한국어 발음 듣기",
                                    tint = BrandGreenLight,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        WordCardEffectBadge(effect)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (animateProgress && unseenIncrease > 0 && animatedPoints.isRunning) {
                                Icon(Icons.Default.ArrowUpward, null, tint = BrandGreenLight, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = "$displayPoint / 100 pt",
                                style = MaterialTheme.typography.labelSmall,
                                color = effect.borderColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val displayDefinition = card.definitionTranslated ?: card.definition
                if (!displayDefinition.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = displayDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                }

                if (!card.definitionEnglish.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.definitionEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                            modifier = Modifier.weight(1f)
                        )

                        if (!card.defTransAudioPath.isNullOrBlank()) {
                            IconButton(
                                onClick = { onPlayTransTts(card) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "번역 뜻 듣기",
                                    tint = DarkMutedText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { displayPoint / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = effect.progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        // 💡 레벨업 오버레이 효과
        AnimatedVisibility(
            visible = showLevelUp,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = effect.borderColor,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "LEVEL UP! 도달: ${newLevel}단계",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}