package com.interactiveword.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.components.MissionCardItem
import com.interactiveword.ui.components.UserHeader
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
                        Text("말해봐", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    // CaptureService 시작/종료 토글
                    IconButton(onClick = { vm.toggleCaptureService() }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "알림 캡처 서비스",
                            tint = if (uiState.isCaptureServiceRunning) BrandGreenLight
                                   else DarkMutedText,
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
                start  = 16.dp,
                end    = 16.dp,
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 유저 헤더
            uiState.user?.let { user ->
                item { UserHeader(user = user) }
            }

            // 오늘의 미션 (최대 2개)
            if (uiState.dailyMissions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title   = "오늘의 미션",
                        onMore  = { navController.navigate(Screen.Profile.route) },
                    )
                }
                items(uiState.dailyMissions.take(2)) { mission ->
                    MissionCardItem(
                        mission = mission,
                        icon    = Icons.Filled.TrackChanges,
                    )
                }
            }

            // 최근 단어 (최대 4개)
            if (uiState.recentWords.isNotEmpty()) {
                item {
                    SectionHeader(
                        title  = "최근 단어",
                        onMore = { navController.navigate(Screen.Collection.route) },
                    )
                }
                items(uiState.recentWords.take(4)) { card ->
                    WordCardItem(
                        card    = card,
                        compact = true,
                        onClick = { navController.navigate(Screen.WordCard.createRoute(card.id)) },
                    )
                }
            }

            // 빠른 학습 버튼
            item {
                Text("빠른 학습", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    QuickActionCard(
                        label    = "단어 스캔",
                        subLabel = "미디어에서 단어 찾기",
                        icon     = Icons.Filled.Mic,
                        modifier = Modifier.weight(1f),
                        onClick  = { navController.navigate(Screen.Scan.route) },
                    )
                    QuickActionCard(
                        label    = "발음 연습",
                        subLabel = "랜덤 단어로 연습",
                        icon     = Icons.Filled.TrackChanges,
                        modifier = Modifier.weight(1f),
                        onClick  = {
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
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
