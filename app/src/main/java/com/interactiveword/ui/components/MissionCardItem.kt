package com.interactiveword.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.interactiveword.data.model.Mission
import com.interactiveword.ui.theme.BrandGreenDim
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@Composable
fun MissionCardItem(
    mission: Mission,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val progress = if (mission.target > 0) mission.progress / mission.target.toFloat() else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = if (mission.isCompleted) BrandGreenDim
                             else MaterialTheme.colorScheme.surface,
        ),
        border   = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 아이콘
            if (icon != null) {
                Surface(
                    shape  = MaterialTheme.shapes.medium,
                    color  = BrandGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = BrandGreenLight,
                            modifier = Modifier.size(20.dp),
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
                        text  = mission.missionType.toDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (mission.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "완료",
                            tint = BrandGreenLight,
                            modifier = Modifier.size(20.dp),
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
                    progress  = { progress.coerceIn(0f, 1f) },
                    modifier  = Modifier.fillMaxWidth().height(4.dp),
                    color     = BrandGreenLight,
                    trackColor = DarkOutline,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${mission.progress}/${mission.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                )
            }
        }
    }
}

private fun String.toDisplayName(): String = when (this) {
    "daily_pronunciation" -> "일일 발음 연습"
    "daily_scan"          -> "단어 스캔"
    "collect_pos"         -> "품사별 단어 수집"
    "collect_topic"       -> "주제별 단어 수집"
    else                  -> this
}
