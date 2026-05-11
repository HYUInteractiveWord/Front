package com.interactiveword.ui.screens.wordcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.components.WordCardEffectBadge
import com.interactiveword.ui.components.wordCardEffectStyle
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordCardScreen(
    wordId: Int,
    navController: NavController,
    vm: WordCardViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val card = uiState.card

    LaunchedEffect(wordId) {
        vm.loadCard(wordId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.koreanWord ?: "단어 카드") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (card == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandGreenLight)
            }
            return@Scaffold
        }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(borderWidth, effect.borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    card.koreanWord,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                Spacer(Modifier.width(8.dp))
                                WordCardEffectBadge(effect)
                            }

                            if (!card.pronunciation.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    card.pronunciation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkMutedText,
                                )
                            }
                        }

                        IconButton(onClick = { vm.playTts() }) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "발음 듣기",
                                tint = BrandGreenLight,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(effect.borderColor, shape = MaterialTheme.shapes.small),
                        )
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (!card.pos.isNullOrBlank()) {
                                Text(
                                    text = "분류: ${card.pos}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandGreenLight,
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            Text(
                                text = "뜻: ${card.definition ?: "뜻 정보가 없습니다."}",
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            if (!card.definitionEnglish.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Meaning: ${card.definitionEnglish}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkMutedText,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "단어 포인트",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkMutedText,
                            )
                            Text(
                                text = "$displayPoint / 100 pt",
                                style = MaterialTheme.typography.bodyMedium,
                                color = effect.borderColor,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { displayPoint.coerceIn(0, 100) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = effect.progressColor,
                            trackColor = DarkOutline,
                        )

                        if (displayPoint >= 100) {
                            Spacer(Modifier.height(12.dp))
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    androidx.compose.ui.graphics.Color(0xFFFFC107)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "✦",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "MASTER 달성",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF5D3B00),
                                        )
                                        Text(
                                            text = "이 단어는 최고 숙련도에 도달했습니다.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkMutedText,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "최고 발음 정확도",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkMutedText,
                            )
                            Text(
                                text = "${card.bestScore.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandGreenLight,
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "스피킹 연습 ${card.speakingCount}회 · 스캔 ${card.scanCount}회",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Learning Examples", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (!card.exampleSentences.isNullOrEmpty()) {
                card.exampleSentences.forEach { example ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exampleKorean(example),
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                val english = exampleEnglish(example)
                                if (!english.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = english,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkMutedText,
                                    )
                                }
                            }

                            val ttsPath = exampleTtsPath(example)
                            if (!ttsPath.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { vm.playExampleTts(ttsPath) }) {
                                    Icon(
                                        Icons.Filled.VolumeUp,
                                        contentDescription = "예문 듣기",
                                        tint = BrandGreenLight,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "아직 예문 정보가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = BorderStroke(1.dp, DarkOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "발음 평가",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "아직 발음 평가 기록이 없습니다.\n발음 연습을 시작하면 점수와 그래프가 표시됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.startPronunciationPractice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
            ) {
                Icon(Icons.Filled.Mic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("발음 연습 시작", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun exampleKorean(example: Any): String {
    if (example is Map<*, *>) {
        for (key in listOf("korean", "kr", "sentence", "example")) {
            val value = example[key]?.toString()
            if (!value.isNullOrBlank()) return value
        }
    }
    return example.toString()
}

private fun exampleEnglish(example: Any): String? {
    if (example is Map<*, *>) {
        for (key in listOf("english", "en", "translation")) {
            val value = example[key]?.toString()
            if (!value.isNullOrBlank()) return value
        }
    }
    return null
}

private fun exampleTtsPath(example: Any): String? {
    if (example is Map<*, *>) {
        for (key in listOf("tts_audio_path", "audio_path", "ttsPath", "tts_path")) {
            val value = example[key]?.toString()
            if (!value.isNullOrBlank()) return value
        }
    }
    return null
}
