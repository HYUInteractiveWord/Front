package com.interactiveword.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.components.MissionCardItem
import com.interactiveword.data.model.User
import com.interactiveword.ui.theme.BrandAmberLight
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("미션 & 프로필") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.user?.let { user ->
                item {
                    MissionProfileDashboard(
                        user = user,
                        wordsCount = uiState.wordsCount,
                    )
                }
            }
            if (uiState.user == null && uiState.profileStatusMessage != null) {
                item {
                    StatusMessageCard(
                        title = "프로필 정보를 불러오지 못했습니다",
                        message = uiState.profileStatusMessage.orEmpty(),
                    )
                }
            }

            // 오늘의 미션
            item {
                Text("오늘의 미션", style = MaterialTheme.typography.titleMedium)
            }
            if (uiState.missionStatusMessage != null) {
                item {
                    StatusMessageCard(
                        title = "일일 미션 상태",
                        message = uiState.missionStatusMessage.orEmpty(),
                    )
                }
            }
            items(uiState.dailyMissions) { mission ->
                val icon = when (mission.missionType) {
                    "daily_pronunciation" -> Icons.Filled.Mic
                    "daily_scan"         -> Icons.Filled.QrCodeScanner
                    else                 -> Icons.Filled.MenuBook
                }
                MissionCardItem(
                    mission = mission,
                    icon    = icon,
                )
            }

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

            // 전체 미션
            if (uiState.allMissions.size > uiState.dailyMissions.size) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("전체 미션", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.allMissions.drop(uiState.dailyMissions.size)) { mission ->
                    MissionCardItem(mission = mission, icon = Icons.Filled.MenuBook)
                }
            }
        }
    }
}

@Composable
private fun StatusMessageCard(
    title: String,
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DarkMutedText,
            )
        }
    }
}

private data class RankBand(
    val rank: String,
    val minXp: Int,
    val maxXpExclusive: Int?,
)

private val rankBands = listOf(
    RankBand("브론즈", 0, 500),
    RankBand("실버", 500, 1500),
    RankBand("골드", 1500, 3000),
    RankBand("Sapphire", 3000, 6000),
    RankBand("Ruby", 6000, 10000),
    RankBand("Emerald", 10000, 15000),
    RankBand("Amethyst", 15000, 21000),
    RankBand("Pearl", 21000, 28000),
    RankBand("Obsidian", 28000, 36000),
    RankBand("다이아", 36000, null),
)

@Composable
private fun MissionProfileDashboard(
    user: User,
    wordsCount: Int,
) {
    val currentBand = rankBands.firstOrNull { band ->
        user.xp >= band.minXp && (band.maxXpExclusive == null || user.xp < band.maxXpExclusive)
    } ?: rankBands.last()

    val nextBand = rankBands.getOrNull(rankBands.indexOf(currentBand) + 1)
    val progress = if (currentBand.maxXpExclusive == null) {
        1f
    } else {
        val range = (currentBand.maxXpExclusive - currentBand.minXp).coerceAtLeast(1)
        ((user.xp - currentBand.minXp).toFloat() / range).coerceIn(0f, 1f)
    }
    val xpLabel = if (currentBand.maxXpExclusive == null) {
        "${user.xp} XP"
    } else {
        "${user.xp} / ${currentBand.maxXpExclusive} XP"
    }
    val remainLabel = if (nextBand == null || currentBand.maxXpExclusive == null) {
        "최고 랭크에 도달했습니다."
    } else {
        "다음 랭크까지 ${currentBand.maxXpExclusive - user.xp} XP 남음"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = user.rank,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandAmberLight,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "랭크 진행",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                    Text(
                        text = xpLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline,
                )
                Text(
                    text = remainLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "수집 단어",
                    value = wordsCount.toString(),
                    icon = Icons.Filled.MenuBook,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "현재 랭크",
                    value = user.rank,
                    icon = Icons.Filled.WorkspacePremium,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "총 XP",
                    value = user.xp.toString(),
                    icon = Icons.Filled.MilitaryTech,
                )
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = BrandGreenLight.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BrandGreenLight,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = DarkMutedText,
            )
        }
    }
}

@Composable
private fun PosQuizEntryCard(
    onStartClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = BrandGreenLight,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "단어 품사 테스트",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "단어의 품사 맞추기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("테스트 시작")
            }
        }
    }
}

@Composable
private fun VocabQuizEntryCard(
    onStartClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = BrandGreenLight,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "단어 암기 테스트",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "수집한 단어로 실력을 테스트해보세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("테스트 시작")
            }
        }
    }
}
