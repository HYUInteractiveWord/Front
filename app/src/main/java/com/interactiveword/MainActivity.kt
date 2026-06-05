package com.interactiveword

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import com.interactiveword.data.local.LanguageManager
import com.interactiveword.service.AudioCaptureService
import com.interactiveword.ui.navigation.AppNavHost
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.DarkBackground
import com.interactiveword.ui.theme.DarkSurface
import com.interactiveword.ui.theme.GameMintDark
import com.interactiveword.ui.theme.InteractiveWordTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.interactiveword.ui.components.XpGainOverlay
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE,
            ),
        )
        extractYouTubeUrl(intent)?.let { ShareIntentHolder.pendingYoutubeUrl.value = it }
        handleCaptureIntent(intent)
        requestNotificationPermission()
        setContent {
            InteractiveWordTheme {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractYouTubeUrl(intent)?.let { ShareIntentHolder.pendingYoutubeUrl.value = it }
        handleCaptureIntent(intent)
    }

    private fun handleCaptureIntent(intent: Intent?) {
        val startMs = intent?.getLongExtra(AudioCaptureService.EXTRA_START_MS, -1L) ?: -1L
        if (startMs < 0L) return
        val endMs  = intent!!.getLongExtra(AudioCaptureService.EXTRA_END_MS, 0L)
        val uriStr = intent.getStringExtra(AudioCaptureService.EXTRA_URI)
        CaptureIntentHolder.pendingCapture.value = CaptureIntentHolder.CaptureRequest(
            uri     = uriStr?.let { Uri.parse(it) },
            startMs = startMs,
            endMs   = endMs,
        )
        // Reset service notification back to standby
        startService(Intent(this, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_DISMISS
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0,
                )
            }
        }
    }

    private fun startAudioCaptureService() {
        val svcIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }
    }

    private fun extractYouTubeUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return Regex("https?://(?:www\\.)?(?:youtube\\.com/(?:watch\\?[^\\s]+|shorts/[^\\s]+)|youtu\\.be/[^\\s]+)")
            .find(text)?.value
    }
}

private data class NavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(Screen.Home,       R.string.nav_home,       Icons.Filled.Home),
    NavItem(Screen.Dictionary, R.string.nav_dictionary, Icons.Filled.Search),
    NavItem(Screen.Collection, R.string.nav_collection, Icons.Filled.MenuBook),
    NavItem(Screen.Profile,    R.string.nav_missions,   Icons.Filled.Person),
)

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val pendingUrl     by ShareIntentHolder.pendingYoutubeUrl.collectAsState()
    val pendingCapture by CaptureIntentHolder.pendingCapture.collectAsState()

    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Scan.route) { launchSingleTop = true }
        }
    }

    LaunchedEffect(pendingCapture) {
        pendingCapture ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Scan.route) { launchSingleTop = true }
        }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        modifier       = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            // 💡 로그인 페이지가 아닐 때만 BottomNavBar 표시
            if (currentRoute != Screen.Login.route) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(bottom = if (currentRoute != Screen.Login.route) padding.calculateBottomPadding() else 0.dp),
            )
            // 💡 전역 XP 획득 오버레이 추가
            XpGainOverlay()
        }
    }
}

@Composable
private fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier
) {
    var textSize by remember { mutableStateOf(12.sp) }

    Text(
        text = text,
        modifier = modifier,
        fontSize = textSize,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                textSize = (textSize.value * 0.9f).sp
            }
        }
    )
}

// 💡 2. 기존 BottomNavBar 수정
@Composable
private fun BottomNavBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 0.dp,
    ) {
        navItems.forEachIndexed { index, item ->
            // 💡 스캔(Mic) 버튼 로직
            if (index == 2) {
                NavigationBarItem(
                    selected = currentRoute == Screen.Scan.route,
                    onClick  = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Home.route) { saveState = false }
                            launchSingleTop = true
                        }
                    },
                    icon  = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            com.interactiveword.ui.theme.BrandGreenLight,
                                            GameMintDark,
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = stringResource(R.string.nav_scan),
                                tint = Color.White,
                            )
                        }
                    },
                    // 🛠 수정됨: 일반 Text 대신 직접 만든 AutoResizeText 사용!
                    label = {
                        AutoResizeText(text = stringResource(R.string.nav_scan))
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = Color.Transparent,
                        selectedIconColor   = com.interactiveword.ui.theme.BrandGreenLight,
                        selectedTextColor   = com.interactiveword.ui.theme.BrandGreenLight,
                        unselectedIconColor = com.interactiveword.ui.theme.DarkMutedText,
                        unselectedTextColor = com.interactiveword.ui.theme.DarkMutedText,
                    ),
                )
            }

            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick  = {
                    if (currentRoute != item.screen.route) {
                        if (item.screen == Screen.Home) {
                            navController.popBackStack(Screen.Home.route, inclusive = false)
                        } else {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                },
                icon  = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                // 🛠 수정됨: 일반 Text 대신 직접 만든 AutoResizeText 사용!
                label = {
                    AutoResizeText(text = stringResource(item.labelRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor      = com.interactiveword.ui.theme.BrandGreenDim,
                    selectedIconColor   = com.interactiveword.ui.theme.BrandGreenLight,
                    selectedTextColor   = com.interactiveword.ui.theme.BrandGreenLight,
                    unselectedIconColor = com.interactiveword.ui.theme.DarkMutedText,
                    unselectedTextColor = com.interactiveword.ui.theme.DarkMutedText,
                ),
            )
        }
    }
}