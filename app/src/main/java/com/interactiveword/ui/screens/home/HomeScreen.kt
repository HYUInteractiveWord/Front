package com.interactiveword.ui.screens.home

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.util.RankManager
import com.interactiveword.R
import com.interactiveword.data.api.RetrofitClient
import com.interactiveword.data.local.TokenDataStore
import com.interactiveword.data.model.User
import com.interactiveword.ui.components.WordCardItem
import com.interactiveword.ui.components.TutorialPrefs
import com.interactiveword.ui.components.TutorialStepDialog
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline
import kotlinx.coroutines.launch
import android.app.Activity
import com.interactiveword.data.local.LanguageManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showHomeTutorial by remember { mutableStateOf(false) }
    var newSelectedLanguage by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }

    LaunchedEffect(uiState.user?.id) {
        val userId = uiState.user?.id ?: return@LaunchedEffect
        if (TutorialPrefs.shouldShow(context, userId, TutorialPrefs.KEY_HOME)) {
            showHomeTutorial = true
        }
    }

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
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.home_logout),
                            tint = DarkMutedText,
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

        if (showHomeTutorial) {
            TutorialStepDialog(
                imageRes = R.drawable.tutorial_home_ru,
                title = stringResource(R.string.tutorial_home_title),
                body = stringResource(R.string.tutorial_home_body),
                confirmText = stringResource(R.string.action_confirm),
                onConfirm = {
                    uiState.user?.id?.let { userId ->
                        TutorialPrefs.markShown(context, userId, TutorialPrefs.KEY_HOME)
                    }
                    showHomeTutorial = false
                },
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.home_logout_confirm_title)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            TokenDataStore(context).clearToken()
                            RetrofitClient.authToken = null
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }) {
                        Text(stringResource(R.string.home_logout_confirm_yes), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(stringResource(R.string.home_logout_confirm_no))
                    }
                },
            )
        }
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(stringResource(R.string.lang_change_title)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.lang_change_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newSelectedLanguage == "en", onClick = { newSelectedLanguage = "en" })
                            Text("English")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newSelectedLanguage == "ru", onClick = { newSelectedLanguage = "ru" })
                            Text("Русский")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLanguageDialog = false
                        vm.changeLanguage(newSelectedLanguage) {
                            LanguageManager.saveLanguage(context, newSelectedLanguage)
                            (context as? Activity)?.recreate() // 언어 적용을 위한 액티비티 재시작
                        }
                    }) {
                        Text(stringResource(R.string.lang_change_confirm), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.lang_change_cancel)) }
                }
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text(stringResource(R.string.delete_account_title)) },
                text = { Text(stringResource(R.string.delete_account_msg), color = MaterialTheme.colorScheme.error) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteAccountDialog = false
                        vm.deleteAccount {
                            scope.launch {
                                TokenDataStore(context).clearToken()
                                RetrofitClient.authToken = null
                                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                            }
                        }
                    }) {
                        Text(stringResource(R.string.delete_account_confirm), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) { Text(stringResource(R.string.delete_account_cancel)) }
                }
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 💡 1. 통합된 프로필 데시보드 카드
            uiState.user?.let { user ->
                item {
                    HomeProfileDashboardCard(
                        user = user,
                        wordCount = uiState.wordCount,
                        bestWord = uiState.bestPronunciationWord,
                        bestScore = uiState.bestPronunciationScore,
                    )
                }
            }

            if (uiState.recentWords.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_recent_words),
                        onMore = { navController.navigate(Screen.Collection.route) },
                    )
                }
                items(uiState.recentWords.take(4)) { card ->
                    WordCardItem(
                        card = card,
                        compact = true,
                        onPlayTts = { vm.playTts(it.ttsAudioPath) },
                        onPlayTransTts = { vm.playTts(it.defTransAudioPath) },
                        onClick = { navController.navigate(Screen.WordCard.createRoute(card.id)) },
                    )
                }
            }

            item {
                Text(stringResource(R.string.home_quick_learning), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    QuickActionCard(
                        label = stringResource(R.string.home_word_scan),
                        subLabel = stringResource(R.string.home_word_scan_sub),
                        icon = Icons.Filled.Mic,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Scan.route) },
                    )
                    QuickActionCard(
                        label = stringResource(R.string.home_pronunciation_practice),
                        subLabel = stringResource(R.string.home_pronunciation_practice_sub),
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
            item {
                Text(stringResource(R.string.settings_section_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, DarkOutline, MaterialTheme.shapes.large)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_lang_change)) },
                        supportingContent = { Text(stringResource(R.string.settings_lang_sub)) },
                        leadingContent = { Icon(Icons.Filled.Language, contentDescription = null, tint = BrandGreenLight) },
                        modifier = Modifier.clickable {
                            newSelectedLanguage = LanguageManager.getSavedLanguage(context)
                            showLanguageDialog = true
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = DarkOutline)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_reset_tutorial)) },
                        supportingContent = { Text(stringResource(R.string.settings_reset_tutorial_sub)) },
                        leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandAmberLight) },
                        modifier = Modifier.clickable {
                            uiState.user?.id?.let { userId ->
                                TutorialPrefs.resetAllForUser(context, userId)
                                // 현재 화면 튜토리얼도 바로 다시 띄움
                                showHomeTutorial = true
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = DarkOutline)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_delete_account)) },
                        supportingContent = { Text(stringResource(R.string.settings_delete_sub)) },
                        leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { showDeleteAccountDialog = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

private const val PROFILE_REWARD_PREFS = "profile_reward_prefs"
private const val DEFAULT_AVATAR_ID = "avatar_newbie"
private const val DEFAULT_BACKGROUND_ID = "bg_forest"
private const val UNLOCK_XP_STEP = 500

private data class ProfileRewardAsset(
    val id: String,
    val titleRes: Int,
    @DrawableRes val drawableRes: Int,
)

private val profileAvatarAssets = listOf(
    ProfileRewardAsset("avatar_newbie", R.string.profile_asset_avatar_newbie, R.drawable.avatar_newbie),
    ProfileRewardAsset("avatar_reading_bear", R.string.profile_asset_avatar_reading_bear, R.drawable.avatar_reading_bear),
    ProfileRewardAsset("avatar_crown_bear", R.string.profile_asset_avatar_crown_bear, R.drawable.avatar_crown_bear),
    ProfileRewardAsset("avatar_writing_rabbit", R.string.profile_asset_avatar_writing_rabbit, R.drawable.avatar_writing_rabbit),
    ProfileRewardAsset("avatar_headset_cat", R.string.profile_asset_avatar_headset_cat, R.drawable.avatar_headset_cat),
    ProfileRewardAsset("avatar_parrot", R.string.profile_asset_avatar_parrot, R.drawable.avatar_parrot),
    ProfileRewardAsset("avatar_explorer", R.string.profile_asset_avatar_explorer, R.drawable.avatar_explorer),
    ProfileRewardAsset("avatar_ghost", R.string.profile_asset_avatar_ghost, R.drawable.avatar_ghost),
    ProfileRewardAsset("avatar_holy_fox", R.string.profile_asset_avatar_holy_fox, R.drawable.avatar_holy_fox),
    ProfileRewardAsset("avatar_cyberpunk_robot", R.string.profile_asset_avatar_cyberpunk_robot, R.drawable.avatar_cyberpunk_robot),
)

private val profileBackgroundAssets = listOf(
    ProfileRewardAsset("bg_forest", R.string.profile_asset_bg_forest, R.drawable.bg_forest),
    ProfileRewardAsset("bg_library", R.string.profile_asset_bg_library, R.drawable.bg_library),
    ProfileRewardAsset("bg_campfire", R.string.profile_asset_bg_campfire, R.drawable.bg_campfire),
    ProfileRewardAsset("bg_cyberpunk", R.string.profile_asset_bg_cyberpunk, R.drawable.bg_cyberpunk),
    ProfileRewardAsset("bg_goldenlake", R.string.profile_asset_bg_goldenlake, R.drawable.bg_goldenlake),
    ProfileRewardAsset("bg_halloween", R.string.profile_asset_bg_halloween, R.drawable.bg_halloween),
)

// ==========================================
// 통합 프로필 컴포넌트 로직
// ==========================================

@Composable
fun HomeProfileDashboardCard(
    user: User,
    wordCount: Int,
    bestWord: String?,
    bestScore: Int,
) {
    val context = LocalContext.current
    val userKey = user.id.toString()

    // 1. 커스터마이징 로드
    var selectedAvatarId by remember(user.id) {
        mutableStateOf(loadSelectedProfileAsset(context, userKey, "selected_avatar", DEFAULT_AVATAR_ID))
    }
    var selectedBackgroundId by remember(user.id) {
        mutableStateOf(loadSelectedProfileAsset(context, userKey, "selected_background", DEFAULT_BACKGROUND_ID))
    }
    var unlockedAvatars by remember(user.id) {
        mutableStateOf(loadUnlockedProfileAssets(context, userKey, "unlocked_avatars", DEFAULT_AVATAR_ID))
    }
    var unlockedBackgrounds by remember(user.id) {
        mutableStateOf(loadUnlockedProfileAssets(context, userKey, "unlocked_backgrounds", DEFAULT_BACKGROUND_ID))
    }

    val totalTickets = user.xp / UNLOCK_XP_STEP
    val usedTickets = (unlockedAvatars - DEFAULT_AVATAR_ID).size + (unlockedBackgrounds - DEFAULT_BACKGROUND_ID).size
    val remainingTickets = (totalTickets - usedTickets).coerceAtLeast(0)

    val selectedAvatar = profileAvatarAssets.firstOrNull { it.id == selectedAvatarId } ?: profileAvatarAssets.first()
    val selectedBackground = profileBackgroundAssets.firstOrNull { it.id == selectedBackgroundId } ?: profileBackgroundAssets.first()

    // 2. 경험치 및 스탯 계산
    val currentBand = RankManager.getCurrentBand(user.xp)
    val nextBand = RankManager.getNextBand(user.xp)

    val progress = if (currentBand.maxXpExclusive == null) 1f else {
        val range = (currentBand.maxXpExclusive - currentBand.minXp).coerceAtLeast(1)
        ((user.xp - currentBand.minXp).toFloat() / range).coerceIn(0f, 1f)
    }
    val xpLabel = if (currentBand.maxXpExclusive == null) "${user.xp} XP" else "${user.xp} / ${currentBand.maxXpExclusive} XP"
    val remainLabel = if (nextBand == null || currentBand.maxXpExclusive == null) {
        stringResource(R.string.profile_top_rank_reached)
    } else {
        stringResource(R.string.profile_xp_to_next_rank, currentBand.maxXpExclusive - user.xp)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // [영역 1] 아바타 & 배경
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.large),
            ) {
                Image(
                    painter = painterResource(selectedBackground.drawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        painter = painterResource(selectedAvatar.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(3.dp, Color.White, CircleShape),
                    )
                    Column {
                        Text(text = user.username, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        val currentBandForLabel = RankManager.getCurrentBand(user.xp)
                        Text(
                            text = RankManager.getRankLabel(currentBandForLabel.rank),
                            style = MaterialTheme.typography.titleMedium,
                            color = RankManager.getRankColor(currentBandForLabel.rank)
                        )
                        Text(
                            text = stringResource(R.string.profile_reward_ticket_owned, remainingTickets),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f),
                        )
                    }
                }
            }

            // [영역 2] 경험치 바
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BrandGreenLight,
                    trackColor = DarkOutline.copy(alpha = 0.35f),
                )
                Text(text = remainLabel, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
            }

            // [영역 3] 상태 위젯
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProfileStatItem(
                    label = stringResource(R.string.home_stat_words),
                    value = wordCount.toString(),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.weight(1f),
                )
                ProfileStatItem(
                    label = stringResource(R.string.home_stat_best_pronunciation),
                    value = if (bestWord != null) "$bestWord\n$bestScore%" else "-",
                    icon = Icons.Filled.TrackChanges,
                    modifier = Modifier.weight(1f),
                )
                ProfileStatItem(
                    label = stringResource(R.string.home_stat_word_slots),
                    value = user.maxWordSlots.toString(),
                    icon = Icons.Filled.Bolt,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = DarkOutline)

            // [영역 4] 커스터마이징 꾸미기 기능
            Text(
                text = stringResource(R.string.profile_reward_unlock_hint),
                style = MaterialTheme.typography.bodySmall,
                color = DarkMutedText,
            )

            RewardAssetRow(
                title = stringResource(R.string.profile_reward_avatar_section),
                assets = profileAvatarAssets,
                selectedId = selectedAvatarId,
                unlockedIds = unlockedAvatars,
                remainingTickets = remainingTickets,
                isAvatar = true,
                onAssetClick = { asset ->
                    if (asset.id in unlockedAvatars) {
                        selectedAvatarId = asset.id
                        saveSelectedProfileAsset(context, userKey, "selected_avatar", asset.id)
                    } else if (remainingTickets > 0) {
                        val updated = unlockedAvatars + asset.id
                        unlockedAvatars = updated
                        selectedAvatarId = asset.id
                        saveUnlockedProfileAssets(context, userKey, "unlocked_avatars", updated)
                        saveSelectedProfileAsset(context, userKey, "selected_avatar", asset.id)
                    }
                },
            )

            RewardAssetRow(
                title = stringResource(R.string.profile_reward_background_section),
                assets = profileBackgroundAssets,
                selectedId = selectedBackgroundId,
                unlockedIds = unlockedBackgrounds,
                remainingTickets = remainingTickets,
                isAvatar = false,
                onAssetClick = { asset ->
                    if (asset.id in unlockedBackgrounds) {
                        selectedBackgroundId = asset.id
                        saveSelectedProfileAsset(context, userKey, "selected_background", asset.id)
                    } else if (remainingTickets > 0) {
                        val updated = unlockedBackgrounds + asset.id
                        unlockedBackgrounds = updated
                        selectedBackgroundId = asset.id
                        saveUnlockedProfileAssets(context, userKey, "unlocked_backgrounds", updated)
                        saveSelectedProfileAsset(context, userKey, "selected_background", asset.id)
                    }
                },
            )

            HorizontalDivider(color = DarkOutline)

            // [영역 5] 현재 보유 티켓 정보
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Red.copy(alpha = 0.08f))
                    .border(1.dp, Color.Red.copy(alpha = 0.2f), MaterialTheme.shapes.large)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.profile_reward_ticket_owned, remainingTickets),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold
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
                .height(130.dp)
                .padding(horizontal = 4.dp, vertical = 12.dp),
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
            Text(stringResource(R.string.home_view_all), style = MaterialTheme.typography.bodySmall, color = BrandGreenLight)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = BrandGreenLight,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
        }
    }
}

@Composable
private fun RewardAssetRow(
    title: String,
    assets: List<ProfileRewardAsset>,
    selectedId: String,
    unlockedIds: Set<String>,
    remainingTickets: Int,
    isAvatar: Boolean,
    onAssetClick: (ProfileRewardAsset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(assets.size) { index ->
                val asset = assets[index]
                val unlocked = asset.id in unlockedIds
                RewardAssetTile(
                    asset = asset,
                    selected = asset.id == selectedId,
                    unlocked = unlocked,
                    canUnlock = remainingTickets > 0,
                    isAvatar = isAvatar,
                    onClick = { onAssetClick(asset) },
                )
            }
        }
    }
}

@Composable
private fun RewardAssetTile(
    asset: ProfileRewardAsset,
    selected: Boolean,
    unlocked: Boolean,
    canUnlock: Boolean,
    isAvatar: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(104.dp)
            .clickable(enabled = unlocked || canUnlock) { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) BrandGreenLight else DarkOutline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                if (isAvatar) {
                    Image(
                        painter = painterResource(asset.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(asset.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                if (!unlocked) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(if (canUnlock) R.string.profile_reward_unlock else R.string.profile_reward_locked),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (canUnlock) BrandGreenLight else DarkMutedText,
                        )
                    }
                }
            }

            Text(
                text = stringResource(
                    when {
                        selected -> R.string.profile_reward_selected
                        unlocked -> R.string.profile_reward_owned
                        canUnlock -> R.string.profile_reward_tap_to_unlock
                        else -> R.string.profile_reward_locked
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    selected -> BrandGreenLight
                    unlocked -> DarkMutedText
                    canUnlock -> BrandGreenLight
                    else -> DarkMutedText
                },
            )
        }
    }
}

// SharedPreferences 유틸
private fun loadSelectedProfileAsset(context: Context, userKey: String, key: String, defaultValue: String): String {
    return context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .getString("${userKey}_${key}", defaultValue) ?: defaultValue
}

private fun saveSelectedProfileAsset(context: Context, userKey: String, key: String, value: String) {
    context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .edit().putString("${userKey}_${key}", value).apply()
}

private fun loadUnlockedProfileAssets(context: Context, userKey: String, key: String, defaultId: String): Set<String> {
    return context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .getStringSet("${userKey}_${key}", setOf(defaultId))?.toSet() ?: setOf(defaultId)
}

private fun saveUnlockedProfileAssets(context: Context, userKey: String, key: String, value: Set<String>) {
    context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .edit().putStringSet("${userKey}_${key}", value).apply()
}
