package com.interactiveword

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
        setContent {
            InteractiveWordTheme {
                MainApp()
            }
        }
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(Screen.Home,       "홈",   Icons.Filled.Home),
    NavItem(Screen.Dictionary, "사전", Icons.Filled.Search),
    NavItem(Screen.Collection, "단어장", Icons.Filled.MenuBook),
    NavItem(Screen.Profile,    "미션", Icons.Filled.Person),
)

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(
                navController = navController,
                currentRoute  = currentRoute,
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
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
        // 사전 탭 (인덱스 1) 앞에 스캔 FAB 삽입
        navItems.forEachIndexed { index, item ->
            // 사전 다음에 스캔 버튼 삽입
            if (index == 2) {
                NavigationBarItem(
                    selected = currentRoute == Screen.Scan.route,
                    onClick  = { navController.navigate(Screen.Scan.route) },
                    icon = {
                        Icon(
                            imageVector        = Icons.Filled.Mic,
                            contentDescription = "스캔",
                        )
                    },
                    label   = { Text("스캔") },
                    colors  = NavigationBarItemDefaults.colors(
                        indicatorColor = com.interactiveword.ui.theme.BrandGreen,
                    ),
                )
            }

            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick  = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon  = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor       = com.interactiveword.ui.theme.BrandGreenDim,
                    selectedIconColor    = com.interactiveword.ui.theme.BrandGreenLight,
                    selectedTextColor    = com.interactiveword.ui.theme.BrandGreenLight,
                    unselectedIconColor  = com.interactiveword.ui.theme.DarkMutedText,
                    unselectedTextColor  = com.interactiveword.ui.theme.DarkMutedText,
                ),
            )
        }
    }
}
