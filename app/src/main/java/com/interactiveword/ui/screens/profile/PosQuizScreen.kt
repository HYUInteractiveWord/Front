package com.interactiveword.ui.screens.profile

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
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
    var showExitDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // 💡 정답을 맞췄을 때 진동 발생
    LaunchedEffect(uiState.isAnswerChecked) {
        if (uiState.isAnswerChecked && uiState.selectedAnswer == uiState.currentQuestion?.correctPos) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(50)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.quiz_exit_title)) },
            text = { Text(stringResource(R.string.quiz_exit_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.quiz_exit_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.quiz_exit_continue))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.posquiz_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!uiState.isFinished) {
                            showExitDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
                    audioPath = question.wordAudioPath,
                    defAudioPath = question.definitionAudioPath,
                    selectedAnswer = uiState.selectedAnswer,
                    isAnswerChecked = uiState.isAnswerChecked,
                    options = question.options,
                    onAnswerClick = vm::selectAnswer,
                    onNextClick = vm::goToNextQuestion,
                    onPlayAudio = vm::playTts,
                    onSpeakText = vm::speakText
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
    audioPath: String?,
    defAudioPath: String?,
    selectedAnswer: String?,
    isAnswerChecked: Boolean,
    options: List<String>,
    onAnswerClick: (String) -> Unit,
    onNextClick: () -> Unit,
    onPlayAudio: (String?) -> Unit,
    onSpeakText: (Context, String) -> Unit,
) {
    val progress = if (totalQuestions > 0) (currentIndex + 1) / totalQuestions.toFloat() else 0f
    var pendingAnswer by remember(currentIndex) { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.quiz_question_counter, currentIndex + 1, totalQuestions), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.posquiz_type_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = BrandGreenLight, trackColor = DarkOutline)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, DarkOutline)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = word, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onPlayAudio(audioPath) }) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = BrandGreenLight) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = definition, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (!defAudioPath.isNullOrBlank()) {
                                    onPlayAudio(defAudioPath)
                                } else {
                                    onSpeakText(context, definition)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = null,
                                tint = BrandGreenLight.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        items(options) { option ->
            val colors = optionCardColors(option, correctPos, selectedAnswer, pendingAnswer, isAnswerChecked)
            OutlinedButton(
                onClick = { if (!isAnswerChecked) pendingAnswer = option },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.background, contentColor = colors.content),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 18.dp),
            ) {
                Text(text = getPosString(option), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            if (!isAnswerChecked) {
                Button(
                    onClick = { pendingAnswer?.let { onAnswerClick(it) } },
                    enabled = pendingAnswer != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight)
                ) {
                    Text(stringResource(R.string.action_confirm), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedAnswer == correctPos) stringResource(R.string.quiz_correct_label) else stringResource(R.string.quiz_wrong_label, getPosString(correctPos)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedAnswer == correctPos) BrandGreenLight else ErrorRed,
                    )
                    Button(onClick = onNextClick, modifier = Modifier.fillMaxWidth()) {
                        Text(if (currentIndex + 1 == totalQuestions) stringResource(R.string.quiz_view_results) else stringResource(R.string.quiz_next_question))
                    }
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
                    text = stringResource(R.string.posquiz_local_note),
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
private fun EmptyQuizState(
    padding: PaddingValues,
    message: String,
    emptyReason: PosQuizEmptyReason,
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
                            Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = BrandGreenLight)
                            Text(
                                text = stringResource(R.string.quiz_add_words_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandGreenLight,
                            )
                        }
                    }
                }
                Text(text = stringResource(R.string.posquiz_cannot_start), style = MaterialTheme.typography.titleLarge)
                Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (emptyReason == PosQuizEmptyReason.NO_WORDS) {
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
    pendingAnswer: String?,
    isAnswerChecked: Boolean,
): AnswerOptionColors {
    if (!isAnswerChecked) {
        return if (option == pendingAnswer) {
            AnswerOptionColors(
                background = BrandGreenLight.copy(alpha = 0.14f),
                border = BrandGreenLight,
                content = BrandGreenLight,
            )
        } else {
            AnswerOptionColors(
                background = MaterialTheme.colorScheme.surface,
                border = DarkOutline,
                content = MaterialTheme.colorScheme.onSurface,
            )
        }
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

@Composable
private fun getPosString(pos: String?): String {
    if (pos == null) return ""
    return when {
        pos.contains("명사") -> stringResource(R.string.pos_noun)
        pos.contains("대명사") -> stringResource(R.string.pos_pronoun)
        pos.contains("수사") -> stringResource(R.string.pos_numeral)
        pos.contains("동사") -> stringResource(R.string.pos_verb)
        pos.contains("형용사") -> stringResource(R.string.pos_adjective)
        pos.contains("관형사") -> stringResource(R.string.pos_determiner)
        pos.contains("부사") -> stringResource(R.string.pos_adverb)
        pos.contains("조사") -> stringResource(R.string.pos_particle)
        pos.contains("감탄사") -> stringResource(R.string.pos_interjection)
        else -> pos
    }
}
