package com.interactiveword

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.interactiveword.ui.navigation.AppNavHost
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.DarkBackground
import com.interactiveword.ui.theme.DarkOutline
import com.interactiveword.ui.theme.DarkSurface
import com.interactiveword.ui.theme.InteractiveWordTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractYouTubeUrl(intent)?.let { ShareIntentHolder.pendingYoutubeUrl.value = it }
        setContent {
            InteractiveWordTheme {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractYouTubeUrl(intent)?.let { ShareIntentHolder.pendingYoutubeUrl.value = it }
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
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(Screen.Home,       "홈",    Icons.Filled.Home),
    NavItem(Screen.Dictionary, "사전",  Icons.Filled.Search),
    NavItem(Screen.Collection, "단어장", Icons.Filled.MenuBook),
    NavItem(Screen.Profile,    "미션",  Icons.Filled.Person),
)

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val pendingUrl by ShareIntentHolder.pendingYoutubeUrl.collectAsState()

    // Share intent 도착 시 스캔 화면으로 이동
    // singleTask: 앱이 이미 실행 중이면 onNewIntent → 이 LaunchedEffect가 처리
    // cold start: Login 화면에서 아직 이동 전이면 LoginScreen이 처리
    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Scan.route) { launchSingleTop = true }
        }
        // currentRoute == null 또는 login이면 LoginScreen의 LaunchedEffect가 처리
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        modifier       = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        )
    }
}

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
            if (index == 2) {
                NavigationBarItem(
                    selected = currentRoute == Screen.Scan.route,
                    onClick  = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Home.route) { saveState = false }
                            launchSingleTop = true
                        }
                    },
                    icon  = { Icon(Icons.Filled.Mic, contentDescription = "스캔") },
                    label = { Text("스캔") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = com.interactiveword.ui.theme.BrandGreen,
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
                icon  = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
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
