package com.example.linklive.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.linklive.presentation.auth.AuthScreen
import com.example.linklive.presentation.call.CallMessagesScreen
import com.example.linklive.presentation.call.MeetingPreviewScreen
import com.example.linklive.presentation.call.ParticipantListScreen
import com.example.linklive.presentation.call.viewmodel.CallViewModel
import com.example.linklive.presentation.call.VideoCallGrid
import com.example.linklive.presentation.home.HomeScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    callViewModel: CallViewModel,
    startService: suspend (String, String, Boolean) -> Boolean,
    isLoggedIn: Boolean,
    peerId: String,
    onEndCall: () -> Unit,
    startScreenSharing: () -> Unit,
    stopScreenSharing: () -> Unit
) {
    val startDestination = if (isLoggedIn) {
        if(callViewModel.isServiceStarted) AppDestinations.VIDEO_GRID_SCREEN else AppDestinations.HOME_SCREEN
    } else AppDestinations.AUTH_SCREEN

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
                navController = navController
            )
        }
        composable(
            route = "meetingPreview/{meetingCode}/{name}/{photoUrl}/{isAskToJoin}",
            arguments = listOf(
                navArgument("meetingCode") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("photoUrl") { type = NavType.StringType },
                navArgument("isAskToJoin") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val meetingCode = backStackEntry.arguments?.getString("meetingCode") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val photoUrl = backStackEntry.arguments?.getString("photoUrl") ?: ""
            val isAskToJoin = backStackEntry.arguments?.getBoolean("isAskToJoin") == true

            callViewModel.setRoomId(meetingCode)
            callViewModel.setPeerId(peerId)

            MeetingPreviewScreen(
                viewModel = callViewModel,
                meetingCode = meetingCode,
                name = name,
                photoUrl = photoUrl,
                onBack = {
                    navController.popBackStack()
                    onEndCall()
                },
                onJoinClick = {
                    if(startService(meetingCode, peerId, !isAskToJoin)) {
                        navController.navigate(AppDestinations.VIDEO_GRID_SCREEN) {
                            popUpTo(AppDestinations.HOME_SCREEN) { inclusive = true }
                        }
                        return@MeetingPreviewScreen true
                    } else {
                        onEndCall()
                        return@MeetingPreviewScreen false
                    }
                },
                isAskToJoin = isAskToJoin
            )
        }
        composable(AppDestinations.VIDEO_GRID_SCREEN) {
            VideoCallGrid(
                viewModel = callViewModel,
                onEndCall = {
                    onEndCall()
                    navController.navigate(AppDestinations.HOME_SCREEN) {
                        popUpTo(AppDestinations.VIDEO_GRID_SCREEN) { inclusive = true }
                    }
                },
                startScreenSharing = {
                    startScreenSharing()
                },
                stopScreenSharing = {
                    stopScreenSharing()
                },
                inCallMessages = {
                    navController.navigate(AppDestinations.CALL_MESSAGE_SCREEN)
                },
                participantList = {
                    navController.navigate(AppDestinations.PARTICIPANT_LIST_SCREEN)
                }
            )
        }
        composable(AppDestinations.CALL_MESSAGE_SCREEN) {
            CallMessagesScreen(
                viewModel = callViewModel,
                onClose = {
                    navController.popBackStack()
                },
                onSendMessage = {
                    callViewModel.sendMessage(it)
                }
            )
        }
        composable(AppDestinations.PARTICIPANT_LIST_SCREEN) {
            ParticipantListScreen(
                viewModel = callViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}