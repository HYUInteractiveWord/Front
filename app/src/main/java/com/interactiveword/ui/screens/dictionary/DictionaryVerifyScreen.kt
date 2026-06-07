package com.interactiveword.ui.screens.dictionary

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryVerifyScreen(
    navController: NavController,
    word: String,
    pos: String,
    definition: String,
    source: String,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: DictionaryVerifyViewModel = viewModel(
        factory = DictionaryVerifyViewModel.factory(
            app = app,
            word = word,
            pos = pos,
            definition = definition,
        )
    )
    val uiState by vm.uiState.collectAsState()

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.toggleRecording()
    }

    fun onMicClick() {
        if (uiState.isRecording) {
            vm.toggleRecording()
            return
        }

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            vm.toggleRecording()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            navController.navigate(Screen.Collection.route) {
                popUpTo(
                    if (source == "scan") {
                        Screen.Scan.route
                    } else {
                        Screen.Dictionary.route
                    }
                ) { inclusive = false }
                launchSingleTop = true
            }
            vm.consumeSaveCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.verify_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.verify_back))
                    }
                },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 단어 표시
            Text(
                text = uiState.word,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(24.dp))

            // TTS 버튼 (로딩 중이면 인디케이터 표시)
            if (uiState.isLoadingPreview) {
                CircularProgressIndicator(
                    color = BrandGreenLight,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                IconButton(
                    onClick = { vm.playPreviewAudio() },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.wordcard_listen),
                        tint = BrandGreenLight,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            // 발음 체크 (마이크)
            Button(
                onClick = { onMicClick() },
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording) ErrorRed else BrandGreenLight,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = when {
                    uiState.isRecording -> stringResource(R.string.verify_recording)
                    uiState.isVerifying -> stringResource(R.string.verify_ai_checking)
                    uiState.isMatch == true -> {
                        val spoken = uiState.spokenCorrected ?: uiState.word
                        stringResource(R.string.verify_match, spoken)
                    }
                    uiState.isMatch == false -> {
                        val spoken = (uiState.spokenCorrected ?: uiState.spokenRaw) ?: "알 수 없음"
                        stringResource(R.string.verify_no_match, spoken)
                    }
                    uiState.hasRecordedOnce -> stringResource(R.string.verify_complete)
                    else -> stringResource(R.string.verify_prompt)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (uiState.isMatch == false) {
                    MaterialTheme.colorScheme.error
                } else {
                    DarkMutedText
                },
            )

            val spokenRaw = uiState.spokenRaw
            if (!spokenRaw.isNullOrBlank() && uiState.isMatch == false) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.verify_raw_text, spokenRaw),
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }

            // 하단 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.action_skip))
                }

                Button(
                    onClick = { vm.saveToCollection() },
                    enabled = uiState.hasRecordedOnce && !uiState.isSaving,
                    modifier = Modifier.weight(1.3f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.action_add_to_collection),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}