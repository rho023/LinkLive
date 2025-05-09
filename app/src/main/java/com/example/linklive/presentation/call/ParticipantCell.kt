package com.example.linklive.presentation.call

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.linklive.R
import com.example.linklive.presentation.model.Participant
import com.example.linklive.utils.PeerConnectionUtils
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

@Composable
fun ParticipantCell(participant: Participant, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        participant.videoTrack?.let { track ->
            // Display video track
            VideoRenderer(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(-10f),
                videoTrack = track
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.baseline_account_circle),
            contentDescription = "No video available",
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
        )

        // Audio handling
        if(participant.audioTrack == null) {
            Image(
                painter = painterResource(R.drawable.mute),
                contentDescription = "Audio not available",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        } else {
            DisposableEffect(participant.audioTrack) {
                participant.audioTrack?.setEnabled(true)
                onDispose {
                    participant.audioTrack.setEnabled(false)
                }
            }
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyGridCell(
    modifier: Modifier = Modifier,
    height: Int,
    width: Int,
    participant: Participant
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val pad = 16

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val parentWidth = constraints.maxWidth.toFloat()
        val parentHeight = constraints.maxHeight.toFloat()
        val windowWidth = with(LocalDensity.current) { (100 + pad).dp.toPx() }
        val windowHeight = with(LocalDensity.current) { (160 + pad).dp.toPx() }

        // Initialize the box at the bottom end
        offsetX = parentWidth - windowWidth
        offsetY = parentHeight - windowHeight

        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        offsetX.coerceIn(100f, parentWidth - windowWidth).roundToInt(),
                        offsetY.coerceIn(100f, parentHeight - windowHeight).roundToInt()
                    )
                }
                .width(width.dp)
                .height(height.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            participant.videoTrack?.let { track ->
                LocalCameraPreview(Modifier.fillMaxSize())
            } ?: Image(
                painter = painterResource(id = R.drawable.baseline_account_circle),
                contentDescription = "No video available",
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )

            if (participant.audioTrack == null) {
                Image(
                    painter = painterResource(id = R.drawable.mute),
                    contentDescription = "Audio not available",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            } else {
                DisposableEffect(participant.audioTrack) {
                    participant.audioTrack?.setEnabled(true)
                    onDispose { participant.audioTrack.setEnabled(false) }
                }
            }

            Text(
                text = participant.name,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun CallControlBar(
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onChatClick: () -> Unit,
    onShareScreen: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(8.dp)
        ) {
            // Toggle Video Button
            Button(
                onClick = onToggleVideo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Image(
                    painterResource(R.drawable.video_camera),
                    contentDescription = "Toggle Video",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // Toggle Audio Button
            Button(
                onClick = onToggleAudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Image(
                    painterResource(R.drawable.mute),
                    contentDescription = "Toggle Audio",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // Chat Button
            Button(
                onClick = onChatClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Image(
                    painterResource(R.drawable.chat),
                    contentDescription = "Open Chat",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // Share Screen Button
            Button(
                onClick = onShareScreen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Image(
                    painterResource(R.drawable.screencast),
                    contentDescription = "Share Screen",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // End Call Button
            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(Color.Red)
            ) {
                Image(
                    painterResource(R.drawable.end_call),
                    contentDescription = "End Call"
                )
            }
        }
    }
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