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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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

    // 녹음 관련 상태 관리
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentRecordFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(wordId) {
        vm.loadCard(wordId)
    }

    // 화면 이탈 시 녹음기 정리
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.koreanWord ?: "단어 카드") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // 💡 수정됨: AutoMirrored 적용
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
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
            // 1. 단어 메인 정보 카드
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
                            // 💡 수정됨: AutoMirrored 적용
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
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
                                    Color(0xFFFFC107)
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
                                        color = Color(0xFFFFC107),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "MASTER 달성",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF5D3B00),
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

            // 2. 예문 섹션
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
                                    // 💡 수정됨: AutoMirrored 적용
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "예문 듣기",
                                        tint = BrandGreenLight,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. 발음 평가 결과 표시 섹션 (서버에서 받은 결과 렌더링)
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
                        text = "발음 평가 결과",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (uiState.isEvaluating) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            "정밀 분석 중...",
                            color = DarkMutedText,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
                    } else if (uiState.evalScore != null) {
                        Text(
                            text = "점수: ${uiState.evalScore?.toInt()}점",
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrandGreenLight
                        )
                        if (uiState.isNewBest) {
                            Text(text = "🎉 최고 점수 경신!", color = Color(0xFFFFC107))
                        }

                        // 서버에서 넘어온 그래프 이미지 로딩 (Pitch 파형 비교 그래프)
                        val pitchGraph = uiState.evalGraphs?.get("pitch_graph")
                        if (!pitchGraph.isNullOrEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            val graphUrl = RetrofitClient.resolveStaticUrl(pitchGraph)
                            AsyncImage(
                                model = graphUrl,
                                contentDescription = "음정 그래프",
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }

                        TextButton(onClick = { vm.clearEvaluation() }) {
                            Text("기록 지우기", color = DarkMutedText)
                        }
                    } else {
                        Text(
                            text = "아직 발음 평가 기록이 없습니다.\n아래 녹음 버튼을 눌러 평가를 시작하세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. 녹음 및 서버 전송 버튼
            Button(
                onClick = {
                    if (isRecording) {
                        // 녹음 중지 및 서버 전송
                        isRecording = false
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null

                            // 파일이 정상 생성되었다면 뷰모델을 통해 서버로 쏜다
                            currentRecordFile?.let { vm.submitPronunciation(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // 임시 파일 생성 및 녹음 시작
                        try {
                            val tempFile = File.createTempFile("user_record_", ".wav", context.cacheDir)
                            currentRecordFile = tempFile

                            // 💡 수정됨: 구형 안드로이드 버전 호환성을 위한 MediaRecorder 초기화 분기
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else BrandGreenLight
                ),
            ) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRecording) "녹음 종료 및 제출" else "녹음 시작",
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