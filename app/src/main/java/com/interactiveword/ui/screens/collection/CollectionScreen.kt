package com.interactiveword.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.components.TutorialPrefs
import com.interactiveword.ui.components.TutorialStepDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.repeatOnLifecycle
import com.interactiveword.util.WordCardPointManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    navController: NavController,
    vm: CollectionViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    var showWordCardTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userId) {
        uiState.userId?.let { userId ->
            if (TutorialPrefs.shouldShow(context, userId, TutorialPrefs.KEY_WORD_CARD)) {
                showWordCardTutorial = true
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            vm.loadWords()
        }
    }

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

    // 💡 미확인 상태인 단어들을 "본 것"으로 표시
    LaunchedEffect(uiState.words) {
        val unseenIds = uiState.words
            .filter { WordCardPointManager.isWordUnseen(context, it.id) }
            .map { it.id }
            
        if (unseenIds.isNotEmpty()) {
            // 약간의 지연 후 처리 (애니메이션이 시작된 후 "본 것"으로 기록)
            kotlinx.coroutines.delay(2500)
            WordCardPointManager.markWordsAsSeen(context, unseenIds)
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
        if (showWordCardTutorial) {
            TutorialStepDialog(
                imageRes = R.drawable.tutorial_word_card_ru,
                title = stringResource(R.string.tutorial_word_card_title),
                body = stringResource(R.string.tutorial_word_card_body),
                confirmText = stringResource(R.string.action_confirm),
                onConfirm = {
                    uiState.userId?.let { userId ->
                        TutorialPrefs.markShown(context, userId, TutorialPrefs.KEY_WORD_CARD)
                    }
                    showWordCardTutorial = false
                },
            )
        }
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
                                label = stringResource(R.string.collection_sort_newest),
                                selected = uiState.sortOrder == SortOrder.NEWEST,
                                onClick = { vm.setSortOrder(SortOrder.NEWEST) }
                            )
                            SortChip(
                                label = stringResource(R.string.collection_sort_score),
                                selected = uiState.sortOrder == SortOrder.SCORE,
                                onClick = { vm.setSortOrder(SortOrder.SCORE) }
                            )
                        }
                    }
                }

                items(uiState.words, key = { it.id }) { card ->
                    // 단어장에서 아직 "보지 않은" 신규 단어들만 등장 애니메이션 적용
                    val isUnseen = WordCardPointManager.isWordUnseen(context, card.id)

                    WordCardItem(
                        card    = card,
                        animateProgress = true,
                        animateEntrance = isUnseen,
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
