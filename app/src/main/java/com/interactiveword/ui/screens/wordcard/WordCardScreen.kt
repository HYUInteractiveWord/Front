package com.interactiveword.ui.screens.wordcard

import com.interactiveword.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.wordcard_back))
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
                        }

                        if (!card.pronunciation.isNullOrBlank()) {
                            Text(
                                text = "[ ${card.pronunciation} ]",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkMutedText,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        IconButton(onClick = { vm.playTts() }) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.wordcard_listen),
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.wordcard_meaning, card.definitionTranslated ?: card.definition ?: stringResource(R.string.wordcard_no_meaning)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                if (!card.defTransAudioPath.isNullOrBlank()) {
                                    IconButton(onClick = { vm.playDefinitionTts() }) {
                                        Icon(
                                            Icons.Filled.VolumeUp,
                                            contentDescription = stringResource(R.string.wordcard_play_definition_tts),
                                        )
                                    }
                                }
                            }

                            if (card.definitionTranslated.isNullOrBlank() && !card.definitionEnglish.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.wordcard_english_meaning_format, card.definitionEnglish),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkMutedText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!card.defTransAudioPath.isNullOrBlank()) {
                                        IconButton(
                                            onClick = { vm.playExampleTts(card.defTransAudioPath) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.VolumeUp,
                                                contentDescription = stringResource(R.string.wordcard_translation_listen),
                                                tint = DarkMutedText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
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
                                text = stringResource(R.string.wordcard_point_format, displayPoint),
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
                                text = stringResource(R.string.wordcard_percentage_format, card.bestScore.toInt()),
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

            Text(stringResource(R.string.wordcard_learning_examples), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (!card.exampleSentences.isNullOrEmpty()) {
                card.exampleSentences.forEach { example ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            // 1. 한국어 예문 및 발음 버튼
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = exampleKorean(example),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                val ttsPath = exampleTtsPath(example)
                                if (!ttsPath.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { vm.playExampleTts(ttsPath) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = BrandGreenLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 2. 💡 복구된 부분: 번역 예문 및 번역 오디오 재생 버튼
                            val english = exampleEnglish(example)
                            if (!english.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = english,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkMutedText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val transTtsPath = exampleTransTtsPath(example)
                                    if (!transTtsPath.isNullOrBlank()) {
                                        IconButton(
                                            onClick = { vm.playExampleTts(transTtsPath) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.VolumeUp,
                                                contentDescription = stringResource(R.string.wordcard_translation_listen),
                                                tint = DarkMutedText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
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
                        text = stringResource(R.string.wordcard_no_examples),
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
                                text = stringResource(R.string.pronunciation_result_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandGreenLight,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.pronunciation_score_format, saved.score.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.pronunciation_xp_gained_format, saved.xpGained),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkMutedText,
                            )
                            if (!saved.recordedAt.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.pronunciation_saved_at_format, saved.recordedAt),
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
                                    stringResource(R.string.pronunciation_volume_ok)
                                } else {
                                    stringResource(R.string.pronunciation_volume_low)
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
                            text = stringResource(R.string.pronunciation_result_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandGreenLight,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pronunciation_score_format, result.score.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.pronunciation_xp_gained_format, result.xpGained),
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
                                    stringResource(R.string.pronunciation_volume_ok)
                                } else {
                                    stringResource(R.string.pronunciation_volume_low)
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
                        uiState.isSubmittingPronunciation -> stringResource(R.string.pronunciation_evaluating)
                        uiState.isRecording -> stringResource(R.string.wordcard_stop_recording_and_evaluate)
                        else -> stringResource(R.string.wordcard_start_pronunciation)
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

// 💡 널 체크(null check) 및 문자열 방어 로직 강화됨
private fun exampleKorean(example: Any): String {
    val map = example as? Map<*, *> ?: return example.toString()
    for (key in listOf("korean", "kr", "sentence", "example")) {
        val value = map[key]?.toString()
        if (!value.isNullOrBlank() && value != "null") return value
    }
    return example.toString()
}

private fun exampleEnglish(example: Any): String? {
    val map = example as? Map<*, *> ?: return null
    for (key in listOf("translation", "russian", "ru", "translated", "translated_text", "english", "en")) {
        val value = map[key]?.toString()
        if (!value.isNullOrBlank() && value != "null") return value
    }
    return null
}

private fun exampleTtsPath(example: Any): String? {
    val map = example as? Map<*, *> ?: return null
    for (key in listOf("audio_path", "tts_audio_path", "ttsPath")) {
        val value = map[key]?.toString()
        if (!value.isNullOrBlank() && value != "null") return value
    }
    return null
}

private fun exampleTransTtsPath(example: Any): String? {
    val map = example as? Map<*, *> ?: return null
    for (key in listOf("trans_audio_path", "translation_audio_path", "def_trans_audio_path")) {
        val value = map[key]?.toString()
        if (!value.isNullOrBlank() && value != "null") return value
    }
    return null
}


@SuppressLint("DefaultLocale")
@Composable
private fun PronunciationScoreChart(
    pronunciation: Float,
    formant: Float,
    pitch: Float,
    timing: Float,
    total: Float,
) {
    val items = listOf(
        stringResource(R.string.pronunciation_label_pronunciation) to pronunciation,
        stringResource(R.string.pronunciation_label_formants) to formant,
        stringResource(R.string.pronunciation_label_intonation) to pitch,
        stringResource(R.string.pronunciation_label_speed) to timing,
        stringResource(R.string.pronunciation_label_total) to total,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.pronunciation_visual_analysis),
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