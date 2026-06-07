package com.interactiveword.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleQuizScreen(
    navController: NavController,
    vm: ExampleQuizViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.examplequiz_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandGreenLight)
                }
                uiState.errorMessage != null -> {
                    ErrorMessageView(uiState.errorMessage!!, onRetry = { vm.restartQuiz() })
                }
                uiState.isFinished -> {
                    QuizResultView(
                        correctCount = uiState.correctCount,
                        totalCount = uiState.totalQuestions,
                        submitMessage = uiState.submitResultMessage,
                        submitError = uiState.submitErrorMessage,
                        onDone = { navController.popBackStack() }
                    )
                }
                uiState.currentQuestion != null -> {
                    ExampleQuizContentView(
                        question = uiState.currentQuestion!!,
                        currentIndex = uiState.currentIndex,
                        totalCount = uiState.totalQuestions,
                        selectedAnswer = uiState.selectedAnswer,
                        isAnswerChecked = uiState.isAnswerChecked,
                        onAnswerClick = { vm.selectAnswer(it) },
                        onNextClick = { vm.goToNextQuestion() },
                        onPlayAudio = { vm.playTts(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleQuizContentView(
    question: ExampleQuizQuestion,
    currentIndex: Int,
    totalCount: Int,
    selectedAnswer: String?,
    isAnswerChecked: Boolean,
    onAnswerClick: (String) -> Unit,
    onNextClick: () -> Unit,
    onPlayAudio: (String?) -> Unit
) {
    var pendingAnswer by remember(currentIndex) { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 프로그레스 바
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / totalCount },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.quiz_question_counter, currentIndex + 1, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGreenLight
                )
            }
        }

        // 문제 카드
        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (question.type == ExampleQuizType.KOREAN_TO_TRANSLATION) stringResource(R.string.examplequiz_kr_prompt)
                        else stringResource(R.string.examplequiz_trans_prompt),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandGreenLight
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = question.prompt,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    if (!question.audioPath.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        IconButton(onClick = { onPlayAudio(question.audioPath) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = BrandGreenLight)
                        }
                    }
                }
            }
        }

        // 선택지
        items(question.options) { option ->
            val isCorrect = option == question.correctAnswer
            val isSelected = if (isAnswerChecked) option == selectedAnswer else option == pendingAnswer

            val color = when {
                !isAnswerChecked -> if (isSelected) BrandGreenLight.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                isCorrect -> BrandGreenLight.copy(alpha = 0.2f)
                isSelected -> ErrorRed.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }

            val borderColor = when {
                !isAnswerChecked -> if (isSelected) BrandGreenLight else DarkOutline
                isCorrect -> BrandGreenLight
                isSelected -> ErrorRed
                else -> DarkOutline
            }

            Card(
                onClick = { if (!isAnswerChecked) pendingAnswer = option },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = color),
                border = BorderStroke(if (isSelected || (isAnswerChecked && isCorrect)) 2.dp else 1.dp, borderColor)
            ) {
                Text(
                    text = option,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 하단 버튼 공간
        item {
            Spacer(Modifier.height(16.dp))
            if (!isAnswerChecked) {
                Button(
                    onClick = { pendingAnswer?.let { onAnswerClick(it) } },
                    enabled = pendingAnswer != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight)
                ) {
                    Text(stringResource(R.string.action_confirm), fontWeight = FontWeight.Bold)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (selectedAnswer == question.correctAnswer) stringResource(R.string.quiz_correct_label)
                               else stringResource(R.string.quiz_wrong_label, question.correctAnswer),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedAnswer == question.correctAnswer) BrandGreenLight else ErrorRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onNextClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight)
                    ) {
                        Text(
                            if (currentIndex + 1 < totalCount) stringResource(R.string.quiz_next_question) 
                            else stringResource(R.string.quiz_view_results),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultView(
    correctCount: Int,
    totalCount: Int,
    submitMessage: String?,
    submitError: String?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("테스트 완료!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$totalCount 문제 중 $correctCount 정답!",
            style = MaterialTheme.typography.titleLarge,
            color = BrandGreenLight
        )
        
        Spacer(Modifier.height(32.dp))
        
        if (submitMessage != null) {
            Text(submitMessage, style = MaterialTheme.typography.bodyLarge, color = BrandGreenLight)
        }
        if (submitError != null) {
            Text(submitError, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
        }
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight)
        ) {
            Text("완료", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorMessageView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}
