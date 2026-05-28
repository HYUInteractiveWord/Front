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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
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
                title = { Text(stringResource(R.string.profile_title)) },
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
                        title = stringResource(R.string.profile_failed_to_load),
                        message = uiState.profileStatusMessage.orEmpty(),
                    )
                }
            }

            item {
                Text(stringResource(R.string.profile_today_missions), style = MaterialTheme.typography.titleMedium)
            }
            if (uiState.missionStatusMessage != null) {
                item {
                    StatusMessageCard(
                        title = stringResource(R.string.profile_daily_mission_status),
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

            if (uiState.allMissions.size > uiState.dailyMissions.size) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.profile_all_missions), style = MaterialTheme.typography.titleMedium)
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
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
        }
    }
}

private data class RankBand(
    val rank: String,
    val minXp: Int,
    val maxXpExclusive: Int?,
)

private val rankBands = listOf(
    RankBand("Bronze",    0,     500),
    RankBand("Silver",    500,   1500),
    RankBand("Gold",      1500,  3000),
    RankBand("Sapphire",  3000,  6000),
    RankBand("Ruby",      6000,  10000),
    RankBand("Emerald",   10000, 15000),
    RankBand("Amethyst",  15000, 21000),
    RankBand("Pearl",     21000, 28000),
    RankBand("Obsidian",  28000, 36000),
    RankBand("Diamond",   36000, null),
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
        stringResource(R.string.profile_top_rank_reached)
    } else {
        stringResource(R.string.profile_xp_to_next_rank, currentBand.maxXpExclusive - user.xp)
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
                Text(text = user.username, style = MaterialTheme.typography.headlineSmall)
                Text(text = user.rank, style = MaterialTheme.typography.titleMedium, color = BrandAmberLight)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.profile_rank_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkMutedText,
                    )
                    Text(text = xpLabel, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline,
                )
                Text(text = remainLabel, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_words),
                    value = wordsCount.toString(),
                    icon = Icons.Filled.MenuBook,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_current_rank),
                    value = user.rank,
                    icon = Icons.Filled.WorkspacePremium,
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_total_xp),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
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
                    Icon(imageVector = icon, contentDescription = null, tint = BrandGreenLight, modifier = Modifier.size(18.dp))
                }
            }
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
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
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.AutoStories, contentDescription = null, tint = BrandGreenLight, modifier = Modifier.size(22.dp))
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
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BrandGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = BrandGreenLight, modifier = Modifier.size(22.dp))
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
