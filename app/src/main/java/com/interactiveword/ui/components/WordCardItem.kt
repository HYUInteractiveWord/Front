package com.interactiveword.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interactiveword.data.model.WordCard
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText

@Composable
fun WordCardItem(
    card: WordCard,
    compact: Boolean = false,
    onPlayTts: (WordCard) -> Unit = {},
    onClick: (WordCard) -> Unit = {},
) {
    val displayPoint = if (card.wordPoint > 0) {
        card.wordPoint
    } else {
        card.bestScore.toInt().coerceIn(0, 100)
    }

    val effect = wordCardEffectStyle(displayPoint)
    val containerColor = effect.containerColor ?: MaterialTheme.colorScheme.surface

    val borderWidth = when {
        displayPoint >= 100 -> 3.dp
        displayPoint >= 76 -> 2.dp
        displayPoint >= 26 -> 1.5.dp
        else -> 1.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(card) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(borderWidth, effect.borderColor),
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
                            style = if (compact) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                        )

                        IconButton(
                            onClick = { onPlayTts(card) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "발음 듣기",
                                tint = BrandGreenLight,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (!card.pronunciation.isNullOrEmpty()) {
                        Text(
                            text = "[${card.pronunciation}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    WordCardEffectBadge(effect)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$displayPoint / 100 pt",
                        style = MaterialTheme.typography.labelSmall,
                        color = effect.borderColor,
                    )
                }
            }

            if (!card.definition.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = card.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkMutedText,
                )
            }

            if (!compact) {
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = BrandGreenLight,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "정확도 ${card.bestScore.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }

                    Text(
                        text = "스캔 ${card.scanCount}회",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkMutedText,
                    )
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { displayPoint.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = effect.progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}