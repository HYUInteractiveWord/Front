package com.interactiveword.ui.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    vm: ScanViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

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
            if (!uiState.isRecording) {
                // 스캔 타입 선택
                Spacer(Modifier.height(48.dp))
                Text(
                    "단어 스캔",
                    style = MaterialTheme.typography.headlineMedium,
                )
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
                        onClick  = { vm.startMicRecording() },
                    )
                    ScanTypeButton(
                        label    = "미디어",
                        subLabel = "영상/음악 소리",
                        icon     = Icons.Filled.OndemandVideo,
                        color    = BrandAmberLight,
                        onClick  = { vm.startMediaCapture() },
                    )
                }
            } else {
                // 녹음 중 UI
                RecordingView(
                    isMic       = uiState.scanType == ScanType.MIC,
                    onStop      = { vm.stopRecording() },
                )
            }

            // 감지된 단어 목록
            if (uiState.detectedWords.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "감지된 단어",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.detectedWords, key = { it }) { word ->
                        DetectedWordItem(
                            word     = word,
                            onAdd    = { vm.addWordToCollection(word) },
                            onDismiss = { vm.dismissWord(word) },
                        )
                    }
                }
            }
        }
    }
}

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
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
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
private fun RecordingView(isMic: Boolean, onStop: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
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
                        tint = BrandGreenLight,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text("듣는 중...", style = MaterialTheme.typography.titleMedium)
    Text(
        "최대 5초간 녹음됩니다",
        style = MaterialTheme.typography.bodyMedium,
        color = DarkMutedText,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(
        onClick = onStop,
        shape   = CircleShape,
    ) {
        Text("중지")
    }
}

@Composable
private fun DetectedWordItem(word: String, onAdd: () -> Unit, onDismiss: () -> Unit) {
    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.VolumeUp, null, tint = BrandGreenLight)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(word, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, null, tint = BrandGreenLight)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, null, tint = DarkMutedText)
            }
        }
    }
}
