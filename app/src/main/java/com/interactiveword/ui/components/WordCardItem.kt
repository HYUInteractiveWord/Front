package com.interactiveword.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interactiveword.data.model.WordCard
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@Composable
fun WordCardItem(
    card: WordCard,
    compact: Boolean = false,
    onPlayTts: (WordCard) -> Unit = {},
    onClick: (WordCard) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(card) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 단어 + TTS 버튼
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = card.koreanWord,
                            style = if (compact) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.titleLarge,
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
                            text  = "[${card.pronunciation}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }
                }

                // 별점 (레벨 1~5)
                Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < card.level) Icons.Filled.Star
                                          else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (i < card.level) BrandAmberLight else DarkOutline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text  = card.definition ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkMutedText,
            )

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
                            text  = "정확도 ${card.bestScore.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }
                    Text(
                        text  = "스캔 ${card.scanCount}회",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkMutedText,
                    )
                }

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { card.level / 5f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color    = BrandAmberLight,
                    trackColor = DarkOutline,
                )
            }
        }
    }
}
