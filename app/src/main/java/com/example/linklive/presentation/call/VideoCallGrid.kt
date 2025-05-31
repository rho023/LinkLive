package com.example.linklive.presentation.call

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.linklive.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.linklive.presentation.call.viewmodel.CallViewModel
import com.example.linklive.utils.SetStatusBarStyle
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VideoCallGrid(
    viewModel: CallViewModel,
    startScreenSharing: () -> Unit,
    stopScreenSharing: () -> Unit,
    inCallMessages: () -> Unit,
    participantList: () -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var isScreenSharing by viewModel.isScreenSharing
    val participantsMap by viewModel.participants.collectAsState()
    val participants = participantsMap.values.toList()
    val focusedParticipant by viewModel.focusedParticipant
    val requestQueue by viewModel.requestQueue.collectAsState()
    val isPipMode by viewModel.isPipMode

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
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No participants",
                color = Color.Black,
                fontSize = 20.sp
            )
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
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if(isPipMode) {
                Log.d("pip", "entering PiP mode")
                AsyncImage(
                    model = if(focusedParticipant != null) focusedParticipant!!.photoUrl else participants[0].photoUrl,
                    contentDescription = "PiP Participant",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(id = R.drawable.baseline_account_circle),
                    error = painterResource(id = R.drawable.baseline_account_circle)
                )
            } else {
                if(isScreenSharing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You are sharing your screen",
                            color = Color.White
                        )

                        MeetingControlRow(
                            viewModel = viewModel,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.TopCenter),
                            onMuteToggle = { isMuted ->
                                viewModel.toggleSpeakerAudio(!isMuted)
                            },
                            onCameraToggle = { viewModel.switchCamera() },
                            onMeetingClick = participantList
                        )

                        var pendingRequest by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
                        var showDialog by rememberSaveable { mutableStateOf(false) }
                        var requesterName by rememberSaveable { mutableStateOf("Unknown") }
                        var coroutineScope = rememberCoroutineScope()

                        LaunchedEffect(requestQueue) {
                            if (pendingRequest == null && requestQueue.isNotEmpty()) {
                                val request = viewModel.dequeRequest() ?: return@LaunchedEffect
                                Log.d("socket", "peer: ${request.first}")

                                coroutineScope.launch {
                                    val userData = viewModel.getDataFromFirestore(request.first.trim())
                                    Log.d("socket", "User data: $userData")
                                    requesterName = userData?.get("name") as? String ?: "Unknown"
                                    pendingRequest = request
                                    showDialog = true
                                }
                            }
                        }

                        if(showDialog && pendingRequest != null) {
                            ShowJoinDialog(requesterName) { userDecision ->
                                viewModel.approvePeer(userDecision, pendingRequest!!.second)
                                showDialog = false
                                pendingRequest = null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .padding(vertical = 34.dp, horizontal = 4.dp)
                                .align(Alignment.BottomCenter)
                                .background(color = Color.Black, shape = RoundedCornerShape(16.dp)),
                        ) {
                            CallControlBar(
                                viewModel = viewModel,
                                onToggleVideo = { viewModel.toggleCamera(it) },
                                onToggleAudio = { viewModel.toggleMic(it) },
                                onShareScreen = { isScreenShare->
                                    if(isScreenShare) {
                                        startScreenSharing()
                                        viewModel.toggleCamera(false)
                                        isScreenSharing = true
                                    }
                                    else {
                                        stopScreenSharing()
                                        viewModel.toggleCamera(true)
                                        isScreenSharing = false
                                    }
                                },
                                onChatClick = inCallMessages,
                                onEndCall = onEndCall
                            )
                        }
                    }
                } else {
                    if(focusedParticipant != null) {
                        ParticipantCell(
                            viewModel = viewModel,
                            participant = focusedParticipant!!,
                            width = screenWidthDp,
                            height = screenHeightDp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
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
                                                        viewModel = viewModel,
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

                            MeetingControlRow(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .align(Alignment.TopCenter),
                                onMuteToggle = { isMuted ->
                                    viewModel.toggleSpeakerAudio(!isMuted)
                                },
                                onCameraToggle = { viewModel.switchCamera() },
                                onMeetingClick = participantList
                            )

                            var pendingRequest by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
                            var showDialog by rememberSaveable { mutableStateOf(false) }
                            var requesterName by rememberSaveable { mutableStateOf("Unknown") }
                            var coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(requestQueue) {
                                if (pendingRequest == null && requestQueue.isNotEmpty()) {
                                    val request = viewModel.dequeRequest() ?: return@LaunchedEffect
                                    Log.d("socket", "peer: ${request.first}")

                                    coroutineScope.launch {
                                        val userData = viewModel.getDataFromFirestore(request.first.trim())
                                        Log.d("socket", "User data: $userData")
                                        requesterName = userData?.get("name") as? String ?: "Unknown"
                                        pendingRequest = request
                                        showDialog = true
                                    }
                                }
                            }

                            if(showDialog && pendingRequest != null) {
                                ShowJoinDialog(requesterName) { userDecision ->
                                    viewModel.approvePeer(userDecision, pendingRequest!!.second)
                                    showDialog = false
                                    pendingRequest = null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp)
                                    .padding(vertical = 34.dp, horizontal = 4.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(color = Color.Black, shape = RoundedCornerShape(16.dp)),
                            ) {
                                CallControlBar(
                                    viewModel = viewModel,
                                    onToggleVideo = { viewModel.toggleCamera(it) },
                                    onToggleAudio = { viewModel.toggleMic(it) },
                                    onShareScreen = { isScreenShare->
                                        if(isScreenShare) {
                                            startScreenSharing()
                                            viewModel.toggleCamera(false)
                                            isScreenSharing = true
                                        }
                                        else {
                                            stopScreenSharing()
                                            viewModel.toggleCamera(true)
                                            isScreenSharing = false
                                        }
                                    },
                                    onChatClick = inCallMessages,
                                    onEndCall = onEndCall
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
fun MeetingControlRow(
    modifier: Modifier,
    onMuteToggle: (Boolean) -> Unit,
    onCameraToggle: () -> Unit,
    onMeetingClick: () -> Unit,
    viewModel: CallViewModel
) {
    var isMuted by rememberSaveable { mutableStateOf(viewModel.isMuted.value) }
    var meetingCode by viewModel.roomId

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(95.dp)
            .padding(vertical = 24.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(3.dp))

        // Meeting Code Button
        Button(
            onClick = onMeetingClick,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.Black,
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = meetingCode,
                color = Color.White,
                fontSize = 17.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(3.dp))

        // Mute Toggle Button
        Button(
            onClick = {
                isMuted = !isMuted
                onMuteToggle(isMuted)
            },
            modifier = Modifier.size(70.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
            ),
            shape = CircleShape
        ) {
            Icon(
                painterResource(id = if(isMuted) R.drawable.mute_speaker else R.drawable.speaker),
                contentDescription = "Mute",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Camera Toggle Button
        Button(
            onClick = {
                onCameraToggle()
            },
            modifier = Modifier.size(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.switch_camera),
                contentDescription = "Toggle Camera",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
    }
}