package com.example.linklive.presentation.call

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linklive.presentation.model.Participant

@Composable
fun VideoCallGrid() {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val viewModel: CallViewModel = hiltViewModel()
    val participants = viewModel.participants.value

    val columns = when {
        participants.size <= 4 -> 1
        else -> 2
    }
    val rows = (participants.size + columns - 1) / columns

    val cellWidth = screenWidthDp / columns
    val cellHeight = screenHeightDp / rows

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (participants.size <= 4) {
            val rows = (participants.size + columns - 1) / columns - 1

            val cellWidth = screenWidthDp / columns
            val cellHeight = screenHeightDp / rows

            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                // Display other participants in a single column
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    for (participantIndex in 1 until participants.size) {
                        ParticipantVideo(
                            participant = participants[participantIndex],
                            width = cellWidth,
                            height = cellHeight
                        )
                    }
                }
                MyGridCell(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    height = 170,
                    width = 100,
                    participant = participants[0]
                )
            }
        } else if(participants.size > 4 && participants.size <= 8) {
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
                                ParticipantVideo(
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
    }
}

@Composable
fun MyGridCell(
    modifier: Modifier,
    height: Int,
    width: Int,
    participant: Participant,
) {
    key(participant) {
        Box(
            modifier = modifier
                .width(width.dp)
                .height(height.dp)
        ) {
            Text("MyGridCell")
        }
    }
}

@Composable
fun ParticipantVideo(participant: Participant, width: Dp, height: Dp) {
    key(participant) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Display participant video or placeholder based on videoEnabled
            if (participant.videoTrack != null) {
                // Show video stream (placeholder for now)
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "${participant.name}'s Video")
                }
            } else {
                // Show avatar/placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = participant.name.first().toString())
                }
            }

            // Status indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                // Audio indicator
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(24.dp)
                        .height(24.dp)
                ) {
                    Text(text = if (participant.audioTrack != null) "🎤" else "🔇")
                }

                // Video indicator
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(24.dp)
                        .height(24.dp)
                ) {
                    Text(text = if (participant.videoTrack != null) "📹" else "🚫")
                }
            }
        }
    }
}