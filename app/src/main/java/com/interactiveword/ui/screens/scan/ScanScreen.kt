package com.interactiveword.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    vm: ScanViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    // 💡 미디어 트리밍을 위한 상태 변수
    var trimmingUri by remember { mutableStateOf<Uri?>(null) }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startMicRecording()
    }

    fun onMicClick() {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            vm.startMicRecording()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopRecording() }
    }

    // 💡 파일 피커: 영상/오디오 선택 시 트리머 다이얼로그 띄움
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            trimmingUri = it
        }
    }

    // 💡 트리머 다이얼로그 렌더링
    trimmingUri?.let { uri ->
        MediaTrimmerDialog(
            uri = uri,
            onDismiss = { trimmingUri = null },
            onConfirm = { startMs, endMs ->
                trimmingUri = null
                vm.startDirectCapture(uri, startMs, endMs)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                uiState.isLoading -> {
                    Spacer(Modifier.height(80.dp))
                    CircularProgressIndicator(color = BrandGreenLight)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when (uiState.scanType) {
                            ScanType.MEDIA -> "미디어에서 오디오를 추출 및 분석 중입니다..."
                            ScanType.YOUTUBE -> stringResource(R.string.scan_loading_youtube)
                            else -> stringResource(R.string.scan_loading)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                }

                uiState.isRecording -> {
                    RecordingView(
                        isMic = uiState.scanType == ScanType.MIC,
                        elapsedSeconds = uiState.elapsedSeconds,
                        onStop = { vm.stopRecording() },
                    )
                }

                else -> {
                    val hasResults = uiState.detectedWords.isNotEmpty()

                    Spacer(Modifier.height(if (hasResults) 8.dp else 48.dp))

                    Text(
                        stringResource(R.string.scan_title),
                        style = if (hasResults) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                    )

                    if (!hasResults) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.scan_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )

                        Spacer(Modifier.height(48.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ScanTypeButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.scan_mic),
                                subLabel = stringResource(R.string.scan_mic_sub),
                                icon = Icons.Filled.Mic,
                                color = BrandGreenLight,
                                onClick = { onMicClick() },
                            )

                            // 💡 미디어 버튼 클릭 시 기존 플레이스홀더 서비스 대신 파일 피커 런처 실행
                            ScanTypeButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.scan_media),
                                subLabel = stringResource(R.string.scan_media_sub),
                                icon = Icons.Filled.OndemandVideo,
                                color = BrandAmberLight,
                                onClick = { mediaPickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                            )
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onMicClick() },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.scan_mic))
                            }

                            OutlinedButton(
                                onClick = { mediaPickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Icon(Icons.Filled.OndemandVideo, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.scan_media))
                            }
                        }
                    }
                }
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.detectedWords.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.scan_detected_words, uiState.detectedWords.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(uiState.detectedWords, key = { it.word }) { result ->
                        DetectedWordItem(
                            result = result,
                            added = result.word in uiState.addedWords,
                            loading = result.word in uiState.loadingWords,
                            onPlay = { vm.playWordAudio(result.word, result.pos, result.definition) },
                            onAdd = { vm.addWordToCollection(result.word, result.pos, result.definition) },
                            onDismiss = { vm.dismissWord(result.word) },
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                vm.cleanupTempFiles()
                                navController.navigate(Screen.Collection.route) {
                                    popUpTo(Screen.Scan.route) { inclusive = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight)
                        ) {
                            Text(stringResource(R.string.scan_collection_complete))
                        }
                    }
                }
            }
        }
    }
}

// 💡 10초 룩백(Lookback) 캡처 다이얼로그
@Composable
fun MediaTrimmerDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current
    var durationMs by remember { mutableStateOf(10000L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }

    val windowSizeMs = 10000L // 💡 10초 캡처 윈도우

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 영상 총 길이 추출
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = timeString?.toLongOrNull()?.coerceAtLeast(1000L) ?: 10000L
                retriever.release()
            } catch (e: Exception) {
                durationMs = 10000L
            }
        }
    }

    // 💡 실시간 재생 위치 업데이트 (드래그 중이 아닐 때만)
    LaunchedEffect(isPlaying, isDragging, mediaPlayer) {
        while (isActive) {
            if (isPlaying && !isDragging) {
                mediaPlayer?.let {
                    currentPositionMs = it.currentPosition.toLong().coerceAtMost(durationMs)
                }
            }
            delay(50) // 부드러운 UI 갱신을 위해 짧은 딜레이
        }
    }

    // 💡 핵심 로직: 현재 위치를 기준으로 이전 10초를 캡처 구간으로 계산
    // 단, 10초 미만인 초반부에서는 무조건 0초~10초 구간으로 고정
    val captureEndMs = if (currentPositionMs < windowSizeMs) {
        minOf(windowSizeMs, durationMs)
    } else {
        currentPositionMs
    }
    val captureStartMs = maxOf(0L, captureEndMs - windowSizeMs)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.scan_media_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.scan_media_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkMutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // 비디오 플레이어 화면 (클릭하여 재생/일시정지 제어 가능)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable {
                            isPlaying = !isPlaying
                            if (isPlaying) mediaPlayer?.start() else mediaPlayer?.pause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(uri)
                                setOnPreparedListener { mp ->
                                    mediaPlayer = mp
                                    mp.start()
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 일시정지 상태일 때 시각적 피드백
                    if (!isPlaying) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OndemandVideo,
                                contentDescription = "Paused",
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 텍스트 인디케이터
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.scan_media_dialog_range_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = DarkMutedText
                        )
                        Text(
                            text = "${formatMs(captureStartMs)} ~ ${formatMs(captureEndMs)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandAmberLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${formatMs(currentPositionMs)} / ${formatMs(durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 💡 커스텀 타임라인 슬라이더
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 배경 트랙 및 10초 구간 하이라이트를 그리는 Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(horizontal = 8.dp) // 슬라이더 썸(Thumb) 패딩 보정
                    ) {
                        val trackWidth = size.width
                        val trackHeight = size.height
                        val cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)

                        // 1. 전체 회색 배경 트랙
                        drawRoundRect(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            size = Size(trackWidth, trackHeight),
                            cornerRadius = cornerRadius
                        )

                        // 2. 10초 캡처 구간 하이라이트 트랙
                        val startFraction = captureStartMs.toFloat() / durationMs.toFloat()
                        val endFraction = captureEndMs.toFloat() / durationMs.toFloat()
                        val highlightWidth = (endFraction - startFraction) * trackWidth
                        val highlightStart = startFraction * trackWidth

                        drawRoundRect(
                            color = BrandAmberLight.copy(alpha = 0.6f),
                            topLeft = Offset(x = highlightStart, y = 0f),
                            size = Size(highlightWidth, trackHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 투명한 트랙을 가진 진짜 슬라이더 (사용자 조작용)
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = {
                            isDragging = true
                            currentPositionMs = it.toLong()

                            // (선택) 드래그 중에는 대략적인 위치만 보여줘서 렉을 방지합니다.
                            // 렉이 심하다면 아래 한 줄은 주석 처리해도 무방합니다.
                            mediaPlayer?.seekTo(it.toInt())
                        },
                        onValueChangeFinished = {
                            isDragging = false

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                mediaPlayer?.seekTo(currentPositionMs, MediaPlayer.SEEK_CLOSEST)
                            } else {
                                mediaPlayer?.seekTo(currentPositionMs.toInt())
                            }

                            if (isPlaying) mediaPlayer?.start()
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent, // Canvas가 보이도록 투명 처리
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = BrandAmberLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    Button(
                        onClick = {
                            // 현재 재생 중이면 정지 후 확정
                            mediaPlayer?.pause()
                            isPlaying = false
                            onConfirm(captureStartMs, captureEndMs)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAmberLight)
                    ) {
                        Text(stringResource(R.string.scan_media_capture_at_current))
                    }
                }
            }
        }
    }
}

// 밀리초(ms)를 mm:ss 형식으로 변환하는 헬퍼 함수
private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}


// --- 하단의 기존 UI 컴포넌트(ScanTypeButton, RecordingView, DetectedWordItem 등)는 그대로 유지됩니다 ---

@Composable
private fun ScanTypeButton(
    modifier: Modifier = Modifier,
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = DarkMutedText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecordingView(
    isMic: Boolean,
    elapsedSeconds: Int,
    onStop: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale",
    )

    Spacer(Modifier.height(48.dp))

    Surface(
        shape = CircleShape,
        color = ErrorRed.copy(alpha = 0.2f),
        modifier = Modifier
            .size(128.dp)
            .scale(scale),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = ErrorRed.copy(alpha = 0.3f),
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isMic) Icons.Filled.Mic else Icons.Filled.OndemandVideo,
                        contentDescription = "녹음 중",
                        tint = ErrorRed,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.scan_listening),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.scan_elapsed, elapsedSeconds),
        style = MaterialTheme.typography.bodyMedium,
        color = DarkMutedText,
    )
    Spacer(Modifier.height(24.dp))

    OutlinedButton(
        onClick = onStop,
        shape = CircleShape,
    ) {
        Text(stringResource(R.string.action_done))
    }
}

@Composable
private fun DetectedWordItem(
    result: ScanWordResult,
    added: Boolean,
    loading: Boolean = false,
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.VolumeUp, null, tint = BrandGreenLight)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(result.word, style = MaterialTheme.typography.titleMedium)

                    result.pos?.let {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = BrandGreenLight.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = getPosString(it),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreenLight,
                            )
                        }
                    }
                }

                result.definition?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkMutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(4.dp),
                    color = BrandGreenLight,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = { if (!added) onAdd() },
                    enabled = !added,
                ) {
                    Icon(
                        if (added) Icons.Filled.Check else Icons.Filled.Add,
                        null,
                        tint = if (added) DarkMutedText else BrandGreenLight,
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, null, tint = DarkMutedText)
            }
        }
    }
}

@Composable
private fun getPosString(pos: String?): String {
    if (pos == null) return ""
    return when {
        pos.contains("명사") -> stringResource(R.string.pos_noun)
        pos.contains("동사") -> stringResource(R.string.pos_verb)
        pos.contains("형용사") -> stringResource(R.string.pos_adjective)
        pos.contains("부사") -> stringResource(R.string.pos_adverb)
        else -> pos
    }
}