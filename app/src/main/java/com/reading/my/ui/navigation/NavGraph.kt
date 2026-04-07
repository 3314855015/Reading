package com.reading.my.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reading.my.ui.screens.login.LoginScreen
import com.reading.my.ui.screens.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBookshelf = {
                    navController.navigate(Screen.Bookshelf.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToReader = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(Screen.Bookshelf.route) {
            HomeScreen(
                onNavigateToBookshelf = {},
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToReader = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(Screen.Search.route) {
            HomeScreen(
                onNavigateToBookshelf = { navController.navigate(Screen.Bookshelf.route) },
                onNavigateToSearch = {},
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToReader = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(Screen.Profile.route) {
            HomeScreen(
                onNavigateToBookshelf = { navController.navigate(Screen.Bookshelf.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToProfile = {},
                onNavigateToReader = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            HomeScreen(
                onNavigateToBookshelf = { navController.navigate(Screen.Bookshelf.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToReader = {}
            )
        }
    }
}
