package com.example.linklive.presentation.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.linklive.R
import com.example.linklive.data.model.Participant
import com.example.linklive.presentation.call.viewmodel.CallViewModel
import com.example.linklive.utils.PeerConnectionUtils
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun ParticipantCell(
    viewModel: CallViewModel,
    participant: Participant,
    width: Dp,
    height: Dp
) {
    val focusedParticipant by viewModel.focusedParticipant

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(focusedParticipant?.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (focusedParticipant != null) viewModel.setFocusedParticipant(null) else viewModel.setFocusedParticipant(
                            participant
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Video handling
        if(!participant.isVideoPaused) {
            VideoRenderer(
                modifier = Modifier
                    .fillMaxSize(),
                videoTrack = participant.videoTrack
            )
        } else {
            AsyncImage(
                model = participant.photoUrl,
                contentDescription = "No video available",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center),
                placeholder = painterResource(R.drawable.baseline_account_circle),
                error = painterResource(R.drawable.baseline_account_circle)
            )
        }

        // Audio handling
        if (participant.isMute) {
            Image(
                painter = painterResource(id = R.drawable.mute),
                contentDescription = "Audio not available",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colorFilter = ColorFilter.tint(Color.Red)
            )
        }

        // Display participant name
        Text(
            text = participant.name,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

@Composable
fun CallControlBar(
    viewModel: CallViewModel,
    onToggleVideo: (Boolean) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onChatClick: () -> Unit,
    onShareScreen: (Boolean) -> Unit,
    onEndCall: () -> Unit
) {
    var isCamEnabled by viewModel.isCamEnabled
    var isMicEnabled by viewModel.isMicEnabled
    var isShareScreenEnabled by viewModel.isScreenSharing

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .background(color = Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Audio/Video Toggle Button
        Spacer(modifier = Modifier.width(4.dp))
        Button(
            onClick = {
                onToggleVideo(!isCamEnabled)},
            modifier = Modifier.weight(1f).fillMaxHeight(),

            shape = CircleShape,
            enabled = !isShareScreenEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if(isCamEnabled) Color.Black.copy(alpha = 0.7f) else Color.White,
                disabledContainerColor = if(isCamEnabled) Color.Black.copy(alpha = 0.7f).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
            ),
        ) {
            Icon(
                painter = painterResource(id = if(isCamEnabled) R.drawable.video_camera else R.drawable.video_camera_prohibited),
                contentDescription = "Audio/Video Toggle",
                tint = if(isCamEnabled) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Audio Toggle Button
        Button(
            onClick = {
                onToggleAudio(!isMicEnabled)},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(isMicEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = if (isMicEnabled) R.drawable.mic else R.drawable.mute),
                contentDescription = "Audio Toggle",
                tint = if(isMicEnabled) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Screen Share Button
        Button(
            onClick = {
                onShareScreen(!isShareScreenEnabled)
                isCamEnabled = !isShareScreenEnabled},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(!isShareScreenEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.screencast),
                contentDescription = "Screen Share",
                tint = if(!isShareScreenEnabled) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Chat Button
        Button(
            onClick = onChatClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= Color.Black.copy(alpha = 0.7f)),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat),
                contentDescription = "In-Call Messages",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Leave Call Button (Red Color)
        Button(
            onClick = onEndCall,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= Color.Red),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.end_call),
                contentDescription = "Leave Call",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
    }
}

@Composable
fun ShowJoinDialog(name: String, onResult: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResult(false) },
        title = { Text(text = "$name is asking to join") },
        confirmButton = {
            Button(onClick = { onResult(true)} ) {
                Text("Accept")
            }
        },
        dismissButton = {
            Button(onClick = { onResult(false) }) {
                Text("Reject")
            }
        }
    )
}

@Composable
fun VideoRenderer(
    modifier: Modifier = Modifier,
    videoTrack: VideoTrack?
) {
   val context = LocalContext.current

   val renderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(Unit) {
        renderer.init(PeerConnectionUtils.eglContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(true)

        onDispose {
            renderer.release()
        }
    }

    // Track handling without recomposition
    DisposableEffect(videoTrack) {
        videoTrack?.addSink(renderer)
        onDispose {
            videoTrack?.removeSink(renderer)
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier,
        update = { /* no-op - renderer is stable */ }
    )
}