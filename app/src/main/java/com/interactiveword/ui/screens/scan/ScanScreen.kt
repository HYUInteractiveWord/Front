package com.interactiveword.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.CaptureIntentHolder
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    vm: ScanViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    var pendingCaptureRequest by remember { mutableStateOf<CaptureIntentHolder.CaptureRequest?>(null) }

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

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val captureReq = pendingCaptureRequest ?: return@rememberLauncherForActivityResult
        pendingCaptureRequest = null
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            vm.startDirectCapture(it, captureReq.startMs, captureReq.endMs)
        }
    }

    val pendingCapture by CaptureIntentHolder.pendingCapture.collectAsState()
    LaunchedEffect(pendingCapture) {
        val request = pendingCapture ?: return@LaunchedEffect
        CaptureIntentHolder.pendingCapture.value = null
        if (request.uri != null) {
            vm.startDirectCapture(request.uri, request.startMs, request.endMs)
        } else {
            pendingCaptureRequest = request
        }
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
                            ScanType.MEDIA -> stringResource(R.string.scan_loading_audio)
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

                    if (pendingCaptureRequest != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.scan_capture_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { mediaPickerLauncher.launch(arrayOf("audio/*", "video/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAmberLight),
                        ) {
                            Icon(Icons.Filled.OndemandVideo, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.scan_select_file))
                        }
                    } else if (!hasResults) {
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

                            ScanTypeButton(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.scan_media),
                                subLabel = stringResource(R.string.scan_media_sub),
                                icon = Icons.Filled.OndemandVideo,
                                color = BrandAmberLight,
                                onClick = { vm.startCaptureService() },
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
                                onClick = { vm.startCaptureService() },
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
                            onAdd = { vm.addWordToCollection(result.word) },
                            onDismiss = { vm.dismissWord(result.word) },
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(Screen.Collection.route) {
                                popUpTo(Screen.Scan.route) { inclusive = false }
                            } },
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
            IconButton(onClick = {}) {
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