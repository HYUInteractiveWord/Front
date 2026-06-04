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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabQuizScreen(
    navController: NavController,
    vm: VocabQuizViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vocabquiz_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                EmptyVocabQuizState(
                    padding = padding,
                    message = uiState.errorMessage.orEmpty(),
                    emptyReason = uiState.emptyReason,
                    onBackClick = { navController.popBackStack() },
                    onMoveToDictionary = { navController.navigate(Screen.Dictionary.route) },
                )
            }

            uiState.isFinished -> {
                VocabQuizResultState(
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
                VocabQuizQuestionState(
                    padding = padding,
                    currentIndex = uiState.currentIndex,
                    totalQuestions = uiState.totalQuestions,
                    questionType = question.type,
                    prompt = question.prompt,
                    correctAnswer = question.correctAnswer,
                    options = question.options,
                    selectedAnswer = uiState.selectedAnswer,
                    isAnswerChecked = uiState.isAnswerChecked,
                    onAnswerClick = vm::selectAnswer,
                    onNextClick = vm::goToNextQuestion,
                    onPlayAudio = vm::playTts // 💡 뷰모델의 오디오 재생 함수 연동
                )
            }
        }
    }
}

@Composable
private fun VocabQuizQuestionState(
    padding: PaddingValues,
    currentIndex: Int,
    totalQuestions: Int,
    questionType: VocabQuizType,
    prompt: String,
    correctAnswer: String,
    options: List<String>,
    selectedAnswer: String?,
    isAnswerChecked: Boolean,
    onAnswerClick: (String) -> Unit,
    onNextClick: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    val progress = if (totalQuestions > 0) (currentIndex + 1) / totalQuestions.toFloat() else 0f

    // 💡 선택지 임시 저장 상태 (확인 버튼 클릭 전까지 유지)
    var pendingAnswer by remember(currentIndex) { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
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
                        text = stringResource(R.string.quiz_question_counter, currentIndex + 1, totalQuestions),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (questionType == VocabQuizType.DEFINITION_TO_WORD) {
                            stringResource(R.string.vocabquiz_def_to_word)
                        } else {
                            stringResource(R.string.vocabquiz_word_to_def)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline,
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkOutline),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (questionType == VocabQuizType.DEFINITION_TO_WORD) {
                            stringResource(R.string.vocabquiz_def_prompt)
                        } else {
                            stringResource(R.string.vocabquiz_word_prompt)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // 💡 문제 텍스트 및 스피커 아이콘 배치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onPlayAudio(prompt) }) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = "발음 듣기",
                                tint = BrandGreenLight
                            )
                        }
                    }
                }
            }
        }

        items(options) { option ->
            val colors = optionCardColors(
                option = option,
                correctAnswer = correctAnswer,
                selectedAnswer = selectedAnswer,
                pendingAnswer = pendingAnswer,
                isAnswerChecked = isAnswerChecked,
            )

            OutlinedButton(
                onClick = {
                    if (!isAnswerChecked) {
                        pendingAnswer = option // 💡 정답 제출 전 임시 저장
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colors.border),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.background,
                    contentColor = colors.content,
                    disabledContainerColor = colors.background,
                    disabledContentColor = colors.content,
                ),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 18.dp),
            ) {
                // 💡 선택지 텍스트 및 스피커 아이콘 배치
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onPlayAudio(option) }) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "발음 듣기",
                            tint = colors.content
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (!isAnswerChecked) {
                Button(
                    onClick = { pendingAnswer?.let { onAnswerClick(it) } },
                    enabled = pendingAnswer != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = BrandGreenLight
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm), // 다국어 리소스로 변경
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                // 💡 제출 후 상태: 결과 및 다음 문제 버튼
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (selectedAnswer == correctAnswer) {
                            stringResource(R.string.quiz_correct_label)
                        } else {
                            stringResource(R.string.quiz_wrong_label, correctAnswer)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedAnswer == correctAnswer) BrandGreenLight else ErrorRed,
                    )
                    Button(
                        onClick = onNextClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = BrandGreenLight
                        )
                    ) {
                        Text(
                            text = if (currentIndex + 1 == totalQuestions) stringResource(R.string.quiz_view_results) else stringResource(R.string.quiz_next_question),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabQuizResultState(
    padding: PaddingValues,
    correctCount: Int,
    totalQuestions: Int,
    xp: Int,
    onRestartClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, DarkOutline),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = stringResource(R.string.quiz_complete), style = MaterialTheme.typography.headlineSmall)
                Text(text = stringResource(R.string.quiz_score, correctCount, totalQuestions), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(R.string.quiz_expected_xp, xp), style = MaterialTheme.typography.bodyLarge, color = BrandGreenLight)
                Text(
                    text = stringResource(R.string.vocabquiz_local_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onRestartClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.quiz_try_again))
                }
                OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.quiz_back_to_missions))
                }
            }
        }
    }
}

@Composable
private fun EmptyVocabQuizState(
    padding: PaddingValues,
    message: String,
    emptyReason: VocabQuizEmptyReason,
    onBackClick: () -> Unit,
    onMoveToDictionary: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, DarkOutline),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (emptyReason == VocabQuizEmptyReason.NO_WORDS) {
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
                            Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = BrandGreenLight)
                            Text(
                                text = stringResource(R.string.quiz_add_words_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandGreenLight,
                            )
                        }
                    }
                }
                Text(text = stringResource(R.string.vocabquiz_cannot_start), style = MaterialTheme.typography.titleLarge)
                Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (emptyReason == VocabQuizEmptyReason.NO_WORDS) {
                    Button(onClick = onMoveToDictionary, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.quiz_go_to_dictionary))
                    }
                }
                OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.quiz_back_to_missions))
                }
            }
        }
    }
}

private data class VocabOptionColors(
    val background: Color,
    val border: Color,
    val content: Color,
)

@Composable
private fun optionCardColors(
    option: String,
    correctAnswer: String,
    selectedAnswer: String?,
    pendingAnswer: String?, // 💡 추가됨: 임시 선택 상태 렌더링
    isAnswerChecked: Boolean,
): VocabOptionColors {
    if (!isAnswerChecked) {
        return if (option == pendingAnswer) {
            VocabOptionColors(
                background = BrandGreenLight.copy(alpha = 0.14f),
                border = BrandGreenLight,
                content = BrandGreenLight,
            )
        } else {
            VocabOptionColors(
                background = MaterialTheme.colorScheme.surface,
                border = DarkOutline,
                content = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    return when {
        option == correctAnswer -> VocabOptionColors(
            background = BrandGreenLight.copy(alpha = 0.14f),
            border = BrandGreenLight,
            content = BrandGreenLight,
        )
        option == selectedAnswer -> VocabOptionColors(
            background = ErrorRed.copy(alpha = 0.14f),
            border = ErrorRed,
            content = ErrorRed,
        )
        else -> VocabOptionColors(
            background = MaterialTheme.colorScheme.surface,
            border = DarkOutline,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}