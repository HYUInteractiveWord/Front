package com.interactiveword.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.TrendingUp
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
    //뜻 오디오 재생 콜백
    onPlayTransTts: (WordCard) -> Unit = {},
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

                        // 발음 기호를 단어 옆, 재생 버튼 앞으로 이동
                        if (!card.pronunciation.isNullOrBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "[ ${card.pronunciation} ]",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkMutedText,
                            )
                        }

                        IconButton(
                            onClick = { onPlayTts(card) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            // 💡 수정됨: AutoMirrored 적용
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
                    Text(
                        text = "$displayPoint / 100 pt",
                        style = MaterialTheme.typography.labelSmall,
                        color = effect.borderColor,
                    )
                }
            }

            // 한국어 뜻
            if (!card.definition.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = card.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkMutedText,
                )
            }

            // 번역된 뜻(러시아어/영어) 및 재생 버튼
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