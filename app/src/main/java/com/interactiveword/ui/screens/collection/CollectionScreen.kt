package com.interactiveword.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.ui.components.WordCardItem
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    navController: NavController,
    vm: CollectionViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 단어장") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (uiState.words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(
                    "아직 단어가 없어요.\n스캔이나 사전 검색으로 추가해보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkMutedText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "${uiState.words.size}개의 단어",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                        Text(
                            "슬롯: ${uiState.words.size} / ${uiState.maxSlots}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreenLight,
                        )
                    }
                }
                items(uiState.words, key = { it.id }) { card ->
                    WordCardItem(
                        card    = card,
                        onClick = { navController.navigate(Screen.WordCard.createRoute(card.id)) },
                    )
                }
            }
        }
    }
}
