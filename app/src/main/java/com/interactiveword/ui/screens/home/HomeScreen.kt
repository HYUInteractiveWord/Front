package com.interactiveword.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.data.model.Mission
import com.interactiveword.data.model.User
import com.interactiveword.ui.components.MissionCardItem
import com.interactiveword.ui.components.WordCardItem
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadData()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = BrandGreenLight.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrandGreenLight,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text("말해봄", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleCaptureService() }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "알림 캡처 서비스",
                            tint = if (uiState.isCaptureServiceRunning) BrandGreenLight else DarkMutedText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            uiState.user?.let { user ->
                item {
                    HomeProfileCard(
                        user = user,
                        wordCount = uiState.wordCount,
                        dailyMissions = uiState.dailyMissions,
                    )
                }
            }

            if (uiState.dailyMissions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "오늘의 미션",
                        onMore = { navController.navigate(Screen.Profile.route) },
                    )
                }
                items(uiState.dailyMissions.take(3)) { mission ->
                    MissionCardItem(
                        mission = mission,
                        icon = missionIcon(mission.missionType),
                    )
                }
            }

            if (uiState.recentWords.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "최근 단어",
                        onMore = { navController.navigate(Screen.Collection.route) },
                    )
                }
                items(uiState.recentWords.take(4)) { card ->
                    WordCardItem(
                        card = card,
                        compact = true,
                        onClick = { navController.navigate(Screen.WordCard.createRoute(card.id)) },
                    )
                }
            }

            item {
                Text("빠른 학습", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    QuickActionCard(
                        label = "단어 스캔",
                        subLabel = "미디어에서 단어 찾기",
                        icon = Icons.Filled.Mic,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Scan.route) },
                    )
                    QuickActionCard(
                        label = "발음 연습",
                        subLabel = "최근 단어로 연습",
                        icon = Icons.Filled.TrackChanges,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            uiState.recentWords.firstOrNull()?.let {
                                navController.navigate(Screen.WordCard.createRoute(it.id))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProfileCard(
    user: User,
    wordCount: Int,
    dailyMissions: List<Mission>,
) {
    val currentXp = user.xp
    val nextGoal = nextRankGoal(user.rank, currentXp)
    val progress = if (nextGoal <= 0) 1f else (currentXp.toFloat() / nextGoal.toFloat()).coerceIn(0f, 1f)
    val remainingXp = if (nextGoal <= 0) 0 else (nextGoal - currentXp).coerceAtLeast(0)

    val completedDaily = dailyMissions.count { it.isCompleted || it.progress >= it.target }
    val totalDaily = dailyMissions.size.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = rankLabel(user.rank),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandGreenLight,
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = BrandGreenLight.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "${user.xp} XP",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandGreenLight,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (nextGoal > 0) "$currentXp / $nextGoal XP" else "$currentXp XP",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (remainingXp > 0) "다음 등급까지 ${remainingXp} XP" else "최고 등급",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkMutedText,
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline.copy(alpha = 0.35f),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProfileStatItem(
                    label = "수집 단어",
                    value = "${wordCount}개",
                    icon = Icons.Filled.MenuBook,
                    modifier = Modifier.weight(1f),
                )
                ProfileStatItem(
                    label = "오늘 미션",
                    value = "$completedDaily/$totalDaily",
                    icon = Icons.Filled.TrackChanges,
                    modifier = Modifier.weight(1f),
                )
                ProfileStatItem(
                    label = "단어 슬롯",
                    value = "${user.maxWordSlots}개",
                    icon = Icons.Filled.Bolt,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
        ),
        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandGreenLight,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = DarkMutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, onMore: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onMore) {
            Text("전체보기", style = MaterialTheme.typography.bodySmall, color = BrandGreenLight)
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    subLabel: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkOutline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = BrandGreenLight.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = BrandGreenLight, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
        }
    }
}

private fun rankLabel(rank: String): String {
    return when (rank.lowercase()) {
        "bronze" -> "브론즈"
        "silver" -> "실버"
        "gold" -> "골드"
        "platinum" -> "플래티넘"
        "diamond" -> "다이아"
        "master" -> "마스터"
        else -> rank
    }
}

private fun nextRankGoal(rank: String, currentXp: Int): Int {
    val byRank = when (rank.lowercase()) {
        "bronze" -> 500
        "silver" -> 1500
        "gold" -> 3000
        "platinum" -> 5000
        "diamond" -> 8000
        "master" -> -1
        else -> -1
    }

    if (byRank > currentXp) return byRank

    return when {
        currentXp < 500 -> 500
        currentXp < 1500 -> 1500
        currentXp < 3000 -> 3000
        currentXp < 5000 -> 5000
        currentXp < 8000 -> 8000
        else -> -1
    }
}


private fun missionIcon(missionType: String): ImageVector {
    return when (missionType) {
        "daily_pronunciation" -> Icons.Filled.Mic
        "daily_scan" -> Icons.Filled.TrackChanges
        "daily_word_quiz" -> Icons.Filled.MenuBook
        "daily_collect_noun" -> Icons.Filled.MenuBook
        else -> Icons.Filled.TrackChanges
    }
}
