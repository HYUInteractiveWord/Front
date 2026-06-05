package com.interactiveword.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.components.WordCardItem
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    navController: NavController,
    vm: CollectionViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadWords()
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
                title = { Text(stringResource(R.string.collection_title)) },
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
                    stringResource(R.string.collection_empty),
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
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.collection_word_count, uiState.words.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkMutedText,
                            )
                            Text(
                                stringResource(R.string.collection_slots, uiState.words.size, uiState.maxSlots),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandGreenLight,
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // 💡 정렬 선택 UI
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SortChip(
                                label = "추가순",
                                selected = uiState.sortOrder == SortOrder.NEWEST,
                                onClick = { vm.setSortOrder(SortOrder.NEWEST) }
                            )
                            SortChip(
                                label = "점수순",
                                selected = uiState.sortOrder == SortOrder.SCORE,
                                onClick = { vm.setSortOrder(SortOrder.SCORE) }
                            )
                        }
                    }
                }

                items(uiState.words, key = { it.id }) { card ->
                    // 가장 최근에 추가된 단어 (ID가 가장 큼) 하나만 등장 애니메이션 적용
                    val isNewest = uiState.words.maxByOrNull { it.id }?.id == card.id && uiState.sortOrder == SortOrder.NEWEST

                    WordCardItem(
                        card    = card,
                        animateProgress = true,
                        animateEntrance = isNewest,
                        //재생 콜백 함수 연결
                        onPlayTts = { vm.playTts(it.ttsAudioPath) },
                        onPlayTransTts = { vm.playTransTts(it.defTransAudioPath) },
                        onClick = { navController.navigate(Screen.WordCard.createRoute(card.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) BrandGreenLight else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) BrandGreenLight else DarkOutline),
        modifier = Modifier.height(32.dp)
    ) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else DarkMutedText
            )
        }
    }
}