package com.interactiveword.ui.screens.profile

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.annotation.DrawableRes
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
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

    LaunchedEffect(Unit) { vm.refresh() }

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
                    ProfileRewardCustomizationCard(
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
            if (uiState.allMissions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.profile_all_missions),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(uiState.allMissions) { mission ->
                    val icon = when (mission.missionType) {
                        "daily_pronunciation" -> Icons.Filled.Mic
                        "daily_scan" -> Icons.Filled.QrCodeScanner
                        else -> Icons.Filled.MenuBook
                    }
                    MissionCardItem(
                        mission = mission,
                        icon = icon,
                    )
                }
            }

        }
    }
}


private const val PROFILE_REWARD_PREFS = "profile_reward_prefs"
private const val DEFAULT_AVATAR_ID = "avatar_newbie"
private const val DEFAULT_BACKGROUND_ID = "bg_forest"
private const val UNLOCK_XP_STEP = 1500

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

@Composable
private fun ProfileRewardCustomizationCard(user: User, wordsCount: Int) {
    val context = LocalContext.current
    val userKey = user.id.toString()

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
    val usedTickets =
        (unlockedAvatars - DEFAULT_AVATAR_ID).size +
        (unlockedBackgrounds - DEFAULT_BACKGROUND_ID).size
    val remainingTickets = (totalTickets - usedTickets).coerceAtLeast(0)

    val selectedAvatar = profileAvatarAssets.firstOrNull { it.id == selectedAvatarId }
        ?: profileAvatarAssets.first()
    val selectedBackground = profileBackgroundAssets.firstOrNull { it.id == selectedBackgroundId }
        ?: profileBackgroundAssets.first()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileRewardPreview(
                user = user,
                avatarRes = selectedAvatar.drawableRes,
                backgroundRes = selectedBackground.drawableRes,
                remainingTickets = remainingTickets,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileRewardInfoChip(
                    icon = Icons.Filled.AutoStories,
                    label = stringResource(R.string.profile_reward_stat_words),
                    value = wordsCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                ProfileRewardInfoChip(
                    icon = Icons.Filled.WorkspacePremium,
                    label = stringResource(R.string.profile_reward_stat_rank),
                    value = user.rank,
                    modifier = Modifier.weight(1f),
                )
                ProfileRewardInfoChip(
                    icon = Icons.Filled.MilitaryTech,
                    label = stringResource(R.string.profile_reward_stat_xp),
                    value = user.xp.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.profile_reward_unlock_hint),
                style = MaterialTheme.typography.bodySmall,
                color = DarkMutedText,
            )
            Spacer(Modifier.height(12.dp))
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
        }
    }
}

@Composable
private fun ProfileRewardPreview(
    user: User,
    @DrawableRes avatarRes: Int,
    @DrawableRes backgroundRes: Int,
    remainingTickets: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(MaterialTheme.shapes.large),
    ) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.25f)),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(3.dp, Color.White, CircleShape),
            )
            Column {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.profile_reward_ticket_owned, remainingTickets),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f),
                )
            }
        }
    }
}

@Composable
private fun ProfileRewardInfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = BrandGreenLight,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkMutedText,
            )
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
        border = androidx.compose.foundation.BorderStroke(
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

private fun loadSelectedProfileAsset(
    context: Context,
    userKey: String,
    key: String,
    defaultValue: String,
): String {
    return context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .getString("${userKey}_${key}", defaultValue) ?: defaultValue
}

private fun saveSelectedProfileAsset(
    context: Context,
    userKey: String,
    key: String,
    value: String,
) {
    context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("${userKey}_${key}", value)
        .apply()
}

private fun loadUnlockedProfileAssets(
    context: Context,
    userKey: String,
    key: String,
    defaultId: String,
): Set<String> {
    return context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .getStringSet("${userKey}_${key}", setOf(defaultId))
        ?.toSet()
        ?: setOf(defaultId)
}

private fun saveUnlockedProfileAssets(
    context: Context,
    userKey: String,
    key: String,
    value: Set<String>,
) {
    context.getSharedPreferences(PROFILE_REWARD_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putStringSet("${userKey}_${key}", value)
        .apply()
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = BrandGreenLight,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
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
