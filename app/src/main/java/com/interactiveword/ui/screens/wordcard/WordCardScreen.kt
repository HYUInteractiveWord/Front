package com.interactiveword.ui.screens.wordcard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.togglePronunciationPractice(context)
        }
    }

    LaunchedEffect(wordId) {
        vm.loadCard(wordId, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.koreanWord ?: stringResource(R.string.wordcard_title)) },
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
                modifier = Modifier.fillMaxSize().padding(padding),
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
            displayPoint >= 76  -> 2.dp
            displayPoint >= 26  -> 1.5.dp
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
                                Text(card.koreanWord, style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.width(8.dp))
                                WordCardEffectBadge(effect)
                            }

                            if (!card.pronunciation.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(card.pronunciation, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
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
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
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
                                    text = stringResource(R.string.wordcard_category, card.pos),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandGreenLight,
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            Text(
                                text = stringResource(R.string.wordcard_meaning, card.definition ?: stringResource(R.string.wordcard_no_meaning)),
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
                                text = stringResource(R.string.wordcard_points),
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
                            modifier = Modifier.fillMaxWidth().height(6.dp),
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
                                            text = stringResource(R.string.wordcard_master_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF5D3B00),
                                        )
                                        Text(
                                            text = stringResource(R.string.wordcard_master_desc),
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
                                text = stringResource(R.string.wordcard_best_score),
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
                            text = stringResource(R.string.wordcard_practice_count, card.speakingCount, card.scanCount),
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = exampleKorean(example), style = MaterialTheme.typography.bodyMedium)
                                val english = exampleEnglish(example)
                                if (!english.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = english, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
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

            if (uiState.pronunciationResult == null) {
                uiState.savedPronunciationResult?.let { saved ->
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        border = BorderStroke(1.dp, BrandGreenLight),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "발음 평가 결과",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandGreenLight,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "점수: ${saved.score.toInt()}점",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "획득 XP: +${saved.xpGained}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkMutedText,
                            )
                            if (!saved.recordedAt.isNullOrBlank()) {
                                Text(
                                    text = "저장 시각: ${saved.recordedAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkMutedText,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            PronunciationScoreChart(
                                pronunciation = saved.pronunciation,
                                formant = saved.formant,
                                pitch = saved.pitch,
                                timing = saved.timing,
                                total = saved.score,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (saved.isIntensityGood) {
                                    "음량 상태: 적절함"
                                } else {
                                    "음량 상태: 작게 녹음됨. 조금 더 크게 말해보세요."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkMutedText,
                            )
                        }
                    }
                }
            }

            uiState.pronunciationResult?.let { result ->
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    border = BorderStroke(1.dp, BrandGreenLight),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "발음 평가 결과",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandGreenLight,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "점수: ${result.score.toInt()}점",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "획득 XP: +${result.xpGained}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                        result.details?.let { details ->
                            Spacer(Modifier.height(12.dp))
                            PronunciationScoreChart(
                                pronunciation = details.pronunciation,
                                formant = details.formant,
                                pitch = details.pitch,
                                timing = details.timing,
                                total = result.score,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (details.isIntensityGood) {
                                    "음량 상태: 적절함"
                                } else {
                                    "음량 상태: 작게 녹음됨. 조금 더 크게 말해보세요."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkMutedText,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        vm.togglePronunciationPractice(context)
                    } else {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
            ) {
                Icon(Icons.Filled.Mic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        uiState.isSubmittingPronunciation -> "평가 중..."
                        uiState.isRecording -> "녹음 종료 및 평가 시작"
                        else -> "발음 연습 시작"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}


private fun historyFloat(history: Map<String, Any>?, vararg keys: String): Float? {
    if (history == null) return null
    for (key in keys) {
        val value = history[key]
        when (value) {
            is Number -> return value.toFloat()
            is String -> value.toFloatOrNull()?.let { return it }
        }
    }
    return null
}

private fun historyString(history: Map<String, Any>?, vararg keys: String): String? {
    if (history == null) return null
    for (key in keys) {
        val value = history[key]?.toString()
        if (!value.isNullOrBlank()) return value
    }
    return null
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


@Composable
private fun PronunciationScoreChart(
    pronunciation: Float,
    formant: Float,
    pitch: Float,
    timing: Float,
    total: Float,
) {
    val items = listOf(
        "발음" to pronunciation,
        "포먼트" to formant,
        "억양" to pitch,
        "속도" to timing,
        "총점" to total,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "시각화 분석 결과",
            style = MaterialTheme.typography.titleSmall,
            color = BrandGreenLight,
        )
        Spacer(Modifier.height(8.dp))

        items.forEach { (label, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                    modifier = Modifier.width(52.dp),
                )

                val clamped = score.coerceIn(0f, 100f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .background(DarkOutline, shape = MaterialTheme.shapes.small),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(clamped / 100f)
                            .background(BrandGreenLight, shape = MaterialTheme.shapes.small),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = String.format("%.1f", score),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp),
                )
            }
        }
    }
}
