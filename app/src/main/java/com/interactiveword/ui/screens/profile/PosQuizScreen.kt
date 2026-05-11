package com.interactiveword.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosQuizScreen(
    navController: NavController,
    vm: PosQuizViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("단어 품사 테스트") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                EmptyQuizState(
                    padding = padding,
                    message = uiState.errorMessage.orEmpty(),
                    emptyReason = uiState.emptyReason,
                    onBackClick = { navController.popBackStack() },
                    onMoveToDictionary = { navController.navigate(Screen.Dictionary.route) },
                )
            }

            uiState.isFinished -> {
                QuizResultState(
                    padding = padding,
                    correctCount = uiState.correctCount,
                    totalQuestions = uiState.totalQuestions,
                    xp = vm.calculateLocalXp(),
                    onRestartClick = vm::restartQuiz,
                    onBackClick = { navController.popBackStack() },
                )
            }

            else -> {
                val question = uiState.currentQuestion ?: return@Scaffold
                QuizQuestionState(
                    padding = padding,
                    currentIndex = uiState.currentIndex,
                    totalQuestions = uiState.totalQuestions,
                    word = question.word,
                    definition = question.definition,
                    correctPos = question.correctPos,
                    selectedAnswer = uiState.selectedAnswer,
                    isAnswerChecked = uiState.isAnswerChecked,
                    onAnswerClick = vm::selectAnswer,
                    onNextClick = vm::goToNextQuestion,
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionState(
    padding: PaddingValues,
    currentIndex: Int,
    totalQuestions: Int,
    word: String,
    definition: String,
    correctPos: String,
    selectedAnswer: String?,
    isAnswerChecked: Boolean,
    onAnswerClick: (String) -> Unit,
    onNextClick: () -> Unit,
) {
    val progress = if (totalQuestions > 0) (currentIndex + 1) / totalQuestions.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "문제 ${currentIndex + 1} / $totalQuestions",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "품사 맞추기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline,
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(1.dp, DarkOutline),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(PosQuizViewModel.options) { option ->
            val colors = optionCardColors(
                option = option,
                correctPos = correctPos,
                selectedAnswer = selectedAnswer,
                isAnswerChecked = isAnswerChecked,
            )

            OutlinedButton(
                onClick = { onAnswerClick(option) },
                enabled = !isAnswerChecked,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.background,
                    contentColor = colors.content,
                    disabledContainerColor = colors.background,
                    disabledContentColor = colors.content,
                ),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 18.dp),
            ) {
                Text(
                    text = option,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (isAnswerChecked) {
            item {
                Text(
                    text = if (selectedAnswer == correctPos) {
                        "정답입니다."
                    } else {
                        "정답은 '$correctPos' 입니다."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedAnswer == correctPos) BrandGreenLight else ErrorRed,
                )
            }
            item {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (currentIndex + 1 == totalQuestions) "결과 보기" else "다음 문제")
                }
            }
        }
    }
}

@Composable
private fun QuizResultState(
    padding: PaddingValues,
    correctCount: Int,
    totalQuestions: Int,
    xp: Int,
    onRestartClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, DarkOutline),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "테스트 완료",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "$correctCount / $totalQuestions 정답",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "예상 획득 XP: +$xp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandGreenLight,
                )
                Text(
                    text = "현재 단계에서는 품사 테스트 점수를 로컬로만 계산합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("다시 풀기")
                }
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("미션으로 돌아가기")
                }
            }
        }
    }
}

@Composable
private fun EmptyQuizState(
    padding: PaddingValues,
    message: String,
    emptyReason: PosQuizEmptyReason,
    onBackClick: () -> Unit,
    onMoveToDictionary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, DarkOutline),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (emptyReason == PosQuizEmptyReason.NO_WORDS) {
                    Surface(
                        color = BrandGreenLight.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = BrandGreenLight,
                            )
                            Text(
                                text = "단어장에 단어를 1회 이상 추가해주세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandGreenLight,
                            )
                        }
                    }
                }
                Text(
                    text = "품사 테스트를 시작할 수 없어요",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (emptyReason == PosQuizEmptyReason.NO_WORDS) {
                    Button(
                        onClick = onMoveToDictionary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("사전으로 이동")
                    }
                }
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("미션으로 돌아가기")
                }
            }
        }
    }
}

private data class AnswerOptionColors(
    val background: Color,
    val border: Color,
    val content: Color,
)

@Composable
private fun optionCardColors(
    option: String,
    correctPos: String,
    selectedAnswer: String?,
    isAnswerChecked: Boolean,
): AnswerOptionColors {
    if (!isAnswerChecked) {
        return AnswerOptionColors(
            background = MaterialTheme.colorScheme.surface,
            border = DarkOutline,
            content = MaterialTheme.colorScheme.onSurface,
        )
    }

    return when {
        option == correctPos -> AnswerOptionColors(
            background = BrandGreenLight.copy(alpha = 0.14f),
            border = BrandGreenLight,
            content = BrandGreenLight,
        )

        option == selectedAnswer -> AnswerOptionColors(
            background = ErrorRed.copy(alpha = 0.14f),
            border = ErrorRed,
            content = ErrorRed,
        )

        else -> AnswerOptionColors(
            background = MaterialTheme.colorScheme.surface,
            border = DarkOutline,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
