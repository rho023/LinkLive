package com.example.linklive.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.linklive.presentation.auth.AuthScreen
import com.example.linklive.presentation.call.VideoCallGrid
import com.example.linklive.presentation.home.HomeScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController(), isLoggedIn: Boolean) {
    val startDestination = if (isLoggedIn) AppDestinations.HOME_SCREEN else AppDestinations.AUTH_SCREEN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppDestinations.AUTH_SCREEN) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(AppDestinations.HOME_SCREEN) {
                        popUpTo(AppDestinations.AUTH_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestinations.HOME_SCREEN) {
            HomeScreen(
                onNavigateToJoinRoom = {
                    navController.navigate(AppDestinations.VIDEO_GRID_SCREEN) {
                        popUpTo(AppDestinations.HOME_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestinations.VIDEO_GRID_SCREEN) {
            VideoCallGrid()
        }
    }
}