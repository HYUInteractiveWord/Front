package com.interactiveword.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.components.MissionCardItem
import com.interactiveword.ui.components.UserHeader

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
            // 유저 헤더
            uiState.user?.let { user ->
                item { UserHeader(user = user) }
            }

            // 오늘의 미션
            item {
                Text("오늘의 미션", style = MaterialTheme.typography.titleMedium)
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
