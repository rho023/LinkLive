package com.example.linklive.presentation.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.linklive.presentation.auth.AuthScreen
import com.example.linklive.presentation.call.CallViewModel
import com.example.linklive.presentation.call.VideoCallGrid
import com.example.linklive.presentation.home.HomeScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    callViewModel: CallViewModel,
    startService: () -> Unit,
    isLoggedIn: Boolean
) {
    val startDestination = if (isLoggedIn) AppDestinations.HOME_SCREEN else AppDestinations.AUTH_SCREEN
    Log.d("rho", "AppNavGraph: hi")

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
//                        popUpTo(AppDestinations.HOME_SCREEN) { inclusive = true }
                    }
                    startService()
                }
            )
        }
        composable(AppDestinations.VIDEO_GRID_SCREEN) {
            VideoCallGrid(
                viewModel = callViewModel,
                onToggleVideo = { },
                onToggleAudio = { },
                onChatClick = { },
                onShareScreen = { },
                onEndCall = { callViewModel.endCall() }
            )
        }
    }
}