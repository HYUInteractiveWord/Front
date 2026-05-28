package com.interactiveword.ui.screens.wordcard

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.interactiveword.R
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.ui.components.WordCardEffectBadge
import com.interactiveword.ui.components.wordCardEffectStyle
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import java.io.File
import java.io.IOException

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

    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentRecordFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(wordId) {
        vm.loadCard(wordId)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.koreanWord ?: stringResource(R.string.wordcard_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.verify_back))
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
                                Icons.AutoMirrored.Filled.VolumeUp,
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, Color(0xFFFFC107)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "✦", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFC107))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.wordcard_master_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF5D3B00),
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
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = BrandGreenLight)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, DarkOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.wordcard_eval_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (uiState.isEvaluating) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            stringResource(R.string.wordcard_evaluating),
                            color = DarkMutedText,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
                    } else if (uiState.evalScore != null) {
                        Text(
                            text = stringResource(R.string.wordcard_score, uiState.evalScore?.toInt() ?: 0),
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrandGreenLight
                        )
                        if (uiState.isNewBest) {
                            Text(text = stringResource(R.string.wordcard_new_best), color = Color(0xFFFFC107))
                        }

                        val pitchGraph = uiState.evalGraphs?.get("pitch_graph")
                        if (!pitchGraph.isNullOrEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            val graphUrl = RetrofitClient.resolveStaticUrl(pitchGraph)
                            AsyncImage(
                                model = graphUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }

                        TextButton(onClick = { vm.clearEvaluation() }) {
                            Text(stringResource(R.string.wordcard_clear_record), color = DarkMutedText)
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.wordcard_no_eval),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            currentRecordFile?.let { vm.submitPronunciation(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            val tempFile = File.createTempFile("user_record_", ".wav", context.cacheDir)
                            currentRecordFile = tempFile

                            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaRecorder()
                            }.apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(tempFile.absolutePath)
                                prepare()
                                start()
                            }
                            isRecording = true
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else BrandGreenLight
                ),
            ) {
                Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRecording) stringResource(R.string.wordcard_stop_and_submit) else stringResource(R.string.wordcard_start_recording),
                    style = MaterialTheme.typography.titleMedium
                )
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
