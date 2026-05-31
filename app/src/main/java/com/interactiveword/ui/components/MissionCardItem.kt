package com.interactiveword.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Surface(
                    shape  = MaterialTheme.shapes.medium,
                    color  = BrandGreenLight,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
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
                        text  = missionDisplayName(mission.missionType),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (mission.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
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

@Composable
private fun missionDisplayName(missionType: String): String = when (missionType) {
    "daily_pronunciation" -> stringResource(R.string.mission_daily_pronunciation)
    "daily_scan"          -> stringResource(R.string.mission_daily_scan)
    "daily_word_quiz"     -> stringResource(R.string.mission_daily_word_quiz)
    "daily_collect_noun"  -> stringResource(R.string.mission_daily_collect_noun)
    else -> missionType
}
