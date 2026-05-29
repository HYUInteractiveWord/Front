package com.interactiveword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.interactiveword.data.local.LanguageManager
import com.interactiveword.ui.navigation.AppNavHost
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.DarkBackground
import com.interactiveword.ui.theme.DarkSurface
import com.interactiveword.ui.theme.GameMintDark
import com.interactiveword.ui.theme.InteractiveWordTheme

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
    val pendingUrl by ShareIntentHolder.pendingYoutubeUrl.collectAsState()

    LaunchedEffect(pendingUrl) {
        val url = pendingUrl ?: return@LaunchedEffect
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
                    icon  = {
                        Box(
                            modifier = Modifier
                                .offset(y = (-6).dp)
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
                                contentDescription = "스캔",
                                tint = Color.White,
                            )
                        }
                    },
                    label = { Text("스캔") },
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
                label = { Text(stringResource(item.labelRes)) },
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
