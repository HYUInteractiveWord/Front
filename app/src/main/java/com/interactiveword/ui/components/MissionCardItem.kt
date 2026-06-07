package com.interactiveword.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interactiveword.R
import com.interactiveword.data.model.Mission
import com.interactiveword.ui.theme.BrandGreenDim
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@Composable
fun MissionCardItem(
    mission: Mission,
    icon: ImageVector? = null,
    onClaim: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val progressValue = if (mission.target > 0) mission.progress / mission.target.toFloat() else 0f
    val isReadyToClaim = !mission.isCompleted && mission.progress >= mission.target

    // 💡 발광(Glow) 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isReadyToClaim) {
                    Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = MaterialTheme.shapes.large,
                            spotColor = BrandGreenLight.copy(alpha = glowAlpha),
                            ambientColor = BrandGreenLight.copy(alpha = glowAlpha)
                        )
                        .clickable { onClaim(mission.id) }
                } else Modifier
            ),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = if (mission.isCompleted) BrandGreenDim
                             else if (isReadyToClaim) BrandGreenLight.copy(alpha = 0.1f)
                             else MaterialTheme.colorScheme.surface,
        ),
        border   = androidx.compose.foundation.BorderStroke(
            width = if (isReadyToClaim) 2.dp else 1.dp,
            color = if (isReadyToClaim) BrandGreenLight.copy(alpha = glowAlpha) else DarkOutline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReadyToClaim) 8.dp else 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null || isReadyToClaim) {
                Surface(
                    shape  = MaterialTheme.shapes.medium,
                    color  = if (isReadyToClaim) BrandGreenLight else BrandGreenLight,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isReadyToClaim) Icons.Default.CardGiftcard else (icon ?: Icons.Default.CardGiftcard),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = missionDisplayName(mission.missionType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isReadyToClaim) FontWeight.Bold else FontWeight.Normal
                    )
                    if (mission.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = BrandGreenLight,
                            modifier = Modifier.size(20.dp),
                        )
                    } else if (isReadyToClaim) {
                        Text(
                            text  = "CLAIM XP!",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandGreenLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            text  = "+${mission.xpReward} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandGreenLight,
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress  = { progressValue.coerceIn(0f, 1f) },
                    modifier  = Modifier.fillMaxWidth().height(8.dp),
                    color     = if (isReadyToClaim) BrandGreenLight else BrandGreenLight,
                    trackColor = DarkOutline,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = if (isReadyToClaim) "완료! 탭하여 보상 받기" else "${mission.progress}/${mission.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReadyToClaim) BrandGreenLight else DarkMutedText,
                    fontWeight = if (isReadyToClaim) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}


@Composable
private fun missionDisplayName(missionType: String): String = when (missionType) {
    "daily_pronunciation" -> stringResource(R.string.mission_daily_pronunciation)
    "daily_scan"          -> stringResource(R.string.mission_daily_scan)
    "daily_word_quiz"     -> stringResource(R.string.mission_daily_word_quiz)
    "daily_example_quiz"  -> stringResource(R.string.mission_daily_example_quiz)
    "daily_example_quiz_kr" -> stringResource(R.string.mission_daily_example_quiz)
    "daily_example_quiz_trans" -> stringResource(R.string.mission_daily_example_quiz)
    "daily_collect_noun"  -> stringResource(R.string.mission_daily_collect_noun)
    else -> missionType
}
