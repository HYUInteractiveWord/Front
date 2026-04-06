package com.interactiveword.ui.screens.wordcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.theme.BrandAmberLight
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

    // ViewModel에 wordId 전달
    if (uiState.card == null) vm.loadCard(wordId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.card?.koreanWord ?: "") },
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
        val card = uiState.card ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // 단어 카드 헤더
            Card(
                shape  = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.koreanWord, style = MaterialTheme.typography.headlineMedium)
                            card.pronunciation?.let {
                                Text("[$it]", style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
                            }
                        }
                        // TTS 버튼
                        IconButton(onClick = { vm.playTts() }) {
                            Icon(Icons.Filled.VolumeUp, "발음 듣기", tint = BrandGreenLight)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 품사 뱃지
                    card.pos?.let { pos ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = BrandGreenLight.copy(alpha = 0.15f),
                        ) {
                            Text(
                                pos,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandGreenLight,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(card.definition ?: "", style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(16.dp))

                    // 레벨 별점
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    if (i < card.level) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    null,
                                    tint = if (i < card.level) BrandAmberLight else DarkOutline,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Text(
                            "최고 점수 ${card.bestScore.toInt()}점",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkMutedText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 예문
            if (!card.exampleSentences.isNullOrEmpty()) {
                Text("예문", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                card.exampleSentences.forEach { example ->
                    Card(
                        shape  = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            example.toString(),
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // 발음 연습 버튼
            Button(
                onClick  = { vm.startPronunciationPractice() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = MaterialTheme.shapes.extraLarge,
                colors   = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
            ) {
                Icon(Icons.Filled.Mic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("발음 연습 시작", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
