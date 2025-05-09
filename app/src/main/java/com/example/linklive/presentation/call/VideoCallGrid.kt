package com.example.linklive.presentation.call

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.linklive.utils.SetStatusBarStyle


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VideoCallGrid(
    viewModel: CallViewModel,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onChatClick: () -> Unit,
    onShareScreen: () -> Unit,
    onEndCall: () -> Unit
) {
    val participants by viewModel.participants

    var isControllableBarVisible by remember { mutableStateOf(false) }

    SetStatusBarStyle(true)

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    if (participants.isEmpty()) {
        // Display a placeholder or message when there are no participants
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val columns = when {
        participants.size <= 4 -> 1
        else -> 2
    }
    val rows = (participants.size + columns - 1) / columns

    val cellWidth = screenWidthDp / columns
    val cellHeight = screenHeightDp / rows

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                isControllableBarVisible = !isControllableBarVisible
            }
    ) {
        if (participants.size <= 8) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (colIndex in 0 until columns) {
                            val participantIndex = rowIndex * columns + colIndex
                            if (participantIndex < participants.size) {
                                ParticipantCell(
                                    participant = participants[participantIndex],
                                    width = cellWidth,
                                    height = cellHeight
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isControllableBarVisible) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CallControlBar(
                    onToggleVideo = onToggleVideo,
                    onToggleAudio = onToggleAudio,
                    onChatClick = onChatClick,
                    onShareScreen = onShareScreen,
                    onEndCall = onEndCall
                )
            }

            // Hide the control bar after 2 seconds
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                isControllableBarVisible = false
            }
        }
    }
}