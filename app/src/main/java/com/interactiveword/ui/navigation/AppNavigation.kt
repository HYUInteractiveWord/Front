package com.interactiveword.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interactiveword.ui.screens.collection.CollectionScreen
import com.interactiveword.ui.screens.dictionary.DictionaryScreen
import com.interactiveword.ui.screens.home.HomeScreen
import com.interactiveword.ui.screens.profile.ProfileScreen
import com.interactiveword.ui.screens.scan.ScanScreen
import com.interactiveword.ui.screens.wordcard.WordCardScreen

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Collection : Screen("collection")
    object Scan       : Screen("scan")
    object Dictionary : Screen("dictionary")
    object Profile    : Screen("profile")
    object WordCard   : Screen("word_card/{wordId}") {
        fun createRoute(wordId: Int) = "word_card/$wordId"
    }
}

// Bottom nav에 표시할 탭 4개 (Scan은 중앙 FAB)
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Dictionary,
    Screen.Collection,
    Screen.Profile,
)

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController  = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Collection.route) {
            CollectionScreen(navController = navController)
        }
        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(Screen.Dictionary.route) {
            DictionaryScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(
            route     = Screen.WordCard.route,
            arguments = listOf(navArgument("wordId") { type = NavType.IntType }),
        ) { backStack ->
            val wordId = backStack.arguments?.getInt("wordId") ?: return@composable
            WordCardScreen(wordId = wordId, navController = navController)
        }
    }
}
