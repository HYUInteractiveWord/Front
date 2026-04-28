package com.interactiveword.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    vm: ScanViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    // ── 마이크 권한 런처 ─────────────────────────────────────────────────────
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startMicRecording() }

    fun onMicClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) vm.startMicRecording()
        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // ── 미디어 파일 피커 ──────────────────────────────────────────────────────
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.onMediaSelected(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("단어 스캔") },
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
                        if (uiState.scanType == ScanType.MEDIA) "오디오 추출 및 분석 중..."
                        else "분석 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                }

                uiState.isRecording -> {
                    RecordingView(
                        isMic          = uiState.scanType == ScanType.MIC,
                        elapsedSeconds = uiState.elapsedSeconds,
                        onStop         = { vm.stopRecording() },
                    )
                }

                else -> {
                    Spacer(Modifier.height(48.dp))
                    Text("단어 스캔", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "주변 소리에서 한국어 단어를 찾아보세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                    Spacer(Modifier.height(48.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ScanTypeButton(
                            label    = "마이크",
                            subLabel = "주변 음성 녹음",
                            icon     = Icons.Filled.Mic,
                            color    = BrandGreenLight,
                            onClick  = { onMicClick() },
                        )
                        ScanTypeButton(
                            label    = "미디어",
                            subLabel = "영상/음악 파일",
                            icon     = Icons.Filled.OndemandVideo,
                            color    = BrandAmberLight,
                            onClick  = { mediaPickerLauncher.launch(arrayOf("audio/*", "video/*")) },
                        )
                    }
                }
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(err, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            if (uiState.detectedWords.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "감지된 단어 (${uiState.detectedWords.size}개)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.detectedWords, key = { it.word }) { result ->
                        DetectedWordItem(
                            result    = result,
                            added     = result.word in uiState.addedWords,
                            onAdd     = { vm.addWordToCollection(result.word) },
                            onDismiss = { vm.dismissWord(result.word) },
                        )
                    }
                }
            }
        }
    }

    // ── 미디어 스캔 설정 바텀시트 ────────────────────────────────────────────
    if (uiState.showMediaSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val maxSec = (uiState.mediaTotalMs / 1000L).toInt().coerceAtLeast(10)
        val sliderMax = minOf(maxSec, 60).toFloat()

        ModalBottomSheet(
            onDismissRequest = { vm.dismissMediaSheet() },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text("미디어 스캔 설정", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    uiState.selectedFileName ?: "",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = DarkMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("스캔 구간 (처음부터)", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${uiState.mediaScanDurationSec}초",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandAmberLight,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value         = uiState.mediaScanDurationSec.toFloat(),
                    onValueChange = { vm.updateMediaScanDuration(it.roundToInt()) },
                    valueRange    = 10f..sliderMax,
                    colors        = SliderDefaults.colors(
                        thumbColor       = BrandAmberLight,
                        activeTrackColor = BrandAmberLight,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("10초", style = MaterialTheme.typography.labelSmall, color = DarkMutedText)
                    Text("${sliderMax.toInt()}초", style = MaterialTheme.typography.labelSmall, color = DarkMutedText)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick  = { vm.startMediaScan() },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.extraLarge,
                    colors   = ButtonDefaults.buttonColors(containerColor = BrandAmberLight),
                ) {
                    Text("스캔 시작")
                }
            }
        }
    }
}

// ── 공통 컴포저블 ──────────────────────────────────────────────────────────────

@Composable
private fun ScanTypeButton(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape   = MaterialTheme.shapes.extraLarge,
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border  = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape    = CircleShape,
                color    = color.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
        }
    }
}

@Composable
private fun RecordingView(isMic: Boolean, elapsedSeconds: Int, onStop: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "scale",
    )

    Spacer(Modifier.height(48.dp))
    Surface(
        shape    = CircleShape,
        color    = BrandGreenLight.copy(alpha = 0.2f),
        modifier = Modifier.size(128.dp).scale(scale),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape    = CircleShape,
                color    = BrandGreenLight.copy(alpha = 0.3f),
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isMic) Icons.Filled.Mic else Icons.Filled.OndemandVideo,
                        contentDescription = "녹음 중",
                        tint     = BrandGreenLight,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Text("듣는 중...", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        "${elapsedSeconds}초  /  최대 30초",
        style = MaterialTheme.typography.bodyMedium,
        color = DarkMutedText,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStop, shape = CircleShape) { Text("완료") }
}

@Composable
private fun DetectedWordItem(
    result: ScanWordResult,
    added: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.VolumeUp, null, tint = BrandGreenLight)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(6.dp),
                ) {
                    Text(result.word, style = MaterialTheme.typography.titleMedium)
                    result.pos?.let {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = BrandGreenLight.copy(alpha = 0.15f),
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = BrandGreenLight,
                            )
                        }
                    }
                }
                result.definition?.let {
                    Text(
                        it,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = DarkMutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { if (!added) onAdd() }, enabled = !added) {
                Icon(
                    if (added) Icons.Filled.Check else Icons.Filled.Add,
                    null,
                    tint = if (added) DarkMutedText else BrandGreenLight,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, null, tint = DarkMutedText)
            }
        }
    }
}
