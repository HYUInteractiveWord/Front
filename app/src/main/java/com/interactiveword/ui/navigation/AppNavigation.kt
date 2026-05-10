package com.interactiveword.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.interactiveword.ui.screens.collection.CollectionScreen
import com.interactiveword.ui.screens.dictionary.DictionaryVerifyScreen
import com.interactiveword.ui.screens.dictionary.DictionaryScreen
import com.interactiveword.ui.screens.home.HomeScreen
import com.interactiveword.ui.screens.login.LoginScreen
import com.interactiveword.ui.screens.profile.ProfileScreen
import com.interactiveword.ui.screens.scan.ScanScreen
import com.interactiveword.ui.screens.wordcard.WordCardScreen

sealed class Screen(val route: String) {
    object Login      : Screen("login")
    object Home       : Screen("home")
    object Collection : Screen("collection")
    object Scan       : Screen("scan")
    object Dictionary : Screen("dictionary")
    object DictionaryVerify : Screen("dictionary_verify?word={word}&pos={pos}&definition={definition}&source={source}") {
        fun createRoute(
            word: String,
            pos: String?,
            definition: String?,
            source: String = "dictionary",
        ): String = buildString {
            append("dictionary_verify")
            append("?word=${Uri.encode(word)}")
            append("&pos=${Uri.encode(pos.orEmpty())}")
            append("&definition=${Uri.encode(definition.orEmpty())}")
            append("&source=${Uri.encode(source)}")
        }
    }
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
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
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
        composable(
            route = Screen.DictionaryVerify.route,
            arguments = listOf(
                navArgument("word") { type = NavType.StringType },
                navArgument("pos") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("definition") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = "dictionary"
                },
            ),
        ) { backStack ->
            val args = backStack.arguments ?: return@composable
            DictionaryVerifyScreen(
                navController = navController,
                word = args.getString("word").orEmpty(),
                pos = args.getString("pos").orEmpty(),
                definition = args.getString("definition").orEmpty(),
                source = args.getString("source").orEmpty(),
            )
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
