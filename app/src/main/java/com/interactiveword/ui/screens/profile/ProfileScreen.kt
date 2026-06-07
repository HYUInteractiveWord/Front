package com.interactiveword.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.components.MissionCardItem
import com.interactiveword.ui.components.TutorialPrefs
import com.interactiveword.ui.components.TutorialStepDialog
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.DarkMutedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    var showMissionTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.user?.id) {
        vm.refresh()

        uiState.user?.id?.let { userId ->
            if (TutorialPrefs.shouldShow(context, userId, TutorialPrefs.KEY_MISSION)) {
                showMissionTutorial = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (showMissionTutorial) {
            TutorialStepDialog(
                imageRes = R.drawable.tutorial_mission_ru,
                title = stringResource(R.string.tutorial_mission_title),
                body = stringResource(R.string.tutorial_mission_body),
                confirmText = stringResource(R.string.action_confirm),
                onConfirm = {
                    uiState.user?.id?.let { userId ->
                        TutorialPrefs.markShown(context, userId, TutorialPrefs.KEY_MISSION)
                    }
                    showMissionTutorial = false
                },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 미션 상태 메시지
            if (uiState.missionStatusMessage != null) {
                item {
                    StatusMessageCard(
                        title = stringResource(R.string.profile_daily_mission_status),
                        message = uiState.missionStatusMessage.orEmpty(),
                    )
                }
            }

            // 1. 일일 미션 섹션
            item {
                Text(stringResource(R.string.profile_today_missions), style = MaterialTheme.typography.titleMedium)
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.dailyMissions.forEach { mission ->
                        key(mission.id) {
                            AnimatedContent(
                                targetState = mission,
                                transitionSpec = {
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it } + fadeOut()
                                    )
                                },
                                label = "singleDailyMissionAnimation"
                            ) { targetMission ->
                                val icon = when (targetMission.missionType) {
                                    "daily_pronunciation" -> Icons.Filled.Mic
                                    "daily_scan"         -> Icons.Filled.QrCodeScanner
                                    "daily_example_quiz", "daily_example_quiz_kr", "daily_example_quiz_trans" -> Icons.Filled.AutoStories
                                    else                 -> Icons.Filled.MenuBook
                                }
                                MissionCardItem(
                                    mission = targetMission,
                                    icon    = icon,
                                    onClaim = { vm.claimMission(it) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. 퀴즈 진입 섹션
            item { Spacer(Modifier.height(8.dp)) }
            item {
                PosQuizEntryCard(
                    onStartClick = { navController.navigate(Screen.PosQuiz.route) }
                )
            }
            item {
                VocabQuizEntryCard(
                    onStartClick = { navController.navigate(Screen.VocabQuiz.route) }
                )
            }
            item {
                ExampleQuizEntryCard(
                    onStartClick = { navController.navigate(Screen.ExampleQuiz.route) }
                )
            }

            // 3. 전체 미션 섹션
            if (uiState.allMissions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.profile_all_missions),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.allMissions.forEach { mission ->
                            key(mission.id) {
                                AnimatedContent(
                                    targetState = mission,
                                    transitionSpec = {
                                        (slideInHorizontally { it } + fadeIn()).togetherWith(
                                            slideOutHorizontally { -it } + fadeOut()
                                        )
                                    },
                                    label = "singleAllMissionAnimation"
                                ) { targetMission ->
                                    val icon = when (targetMission.missionType) {
                                        "daily_pronunciation" -> Icons.Filled.Mic
                                        "daily_scan" -> Icons.Filled.QrCodeScanner
                                        "daily_example_quiz", "daily_example_quiz_kr", "daily_example_quiz_trans" -> Icons.Filled.AutoStories
                                        else -> Icons.Filled.MenuBook
                                    }
                                    MissionCardItem(
                                        mission = targetMission,
                                        icon = icon,
                                        onClaim = { vm.claimMission(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 하위 컴포넌트들 (퀴즈 카드 및 상태 메시지)
// ==========================================

@Composable
private fun StatusMessageCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
        }
    }
}

@Composable
private fun PosQuizEntryCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.profile_pos_quiz_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.profile_pos_quiz_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start_test))
            }
        }
    }
}

@Composable
private fun VocabQuizEntryCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.profile_vocab_quiz_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.profile_vocab_quiz_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start_test))
            }
        }
    }
}

@Composable
private fun ExampleQuizEntryCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.profile_example_quiz_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.profile_example_quiz_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start_test))
            }
        }
    }
}
