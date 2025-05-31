package com.example.linklive.presentation.call

import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.linklive.R
import com.example.linklive.presentation.call.viewmodel.CallViewModel
import kotlinx.coroutines.launch

@Composable
fun MeetingPreviewScreen(
    viewModel: CallViewModel,
    meetingCode: String,
    name: String,
    photoUrl: String,
    onBack: () -> Unit,
    onJoinClick: suspend () -> Boolean,
    isAskToJoin: Boolean
) {
    var isMicEnabled by rememberSaveable { mutableStateOf(true) }
    var isCamEnabled by rememberSaveable { mutableStateOf(true) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        Modifier.size(35.dp)
                    )
                }

                IconButton(onClick = {isMuted = !isMuted}) {
                    Icon(
                        painter = painterResource(id = if (isMuted) R.drawable.mute_speaker else R.drawable.speaker),
                        contentDescription = "Mute",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Black
                    )
                }


            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = meetingCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .width(250.dp)
                    .height(350.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if(isCamEnabled){
                    CameraPreviewView(modifier = Modifier.fillMaxSize())
                }else {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "User Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        placeholder = painterResource(id = R.drawable.baseline_account_circle)

                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {isCamEnabled = !isCamEnabled },
                        modifier = Modifier.weight(1f).wrapContentSize(),
                        shape = CircleShape,
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
                        onClick = {isMicEnabled = !isMicEnabled },
                        modifier = Modifier.weight(1f).wrapContentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor= if(isMicEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(id = if (isMicEnabled) R.drawable.mic else R.drawable.mute),
                            contentDescription = "Audio Toggle",
                            tint = if(isMicEnabled) Color.White else Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            HorizontalDivider(thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.information),
                        contentDescription = "Info",
                        modifier = Modifier
                            .size(24.dp),
                        tint = Color.Black,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Joining information",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, meetingCode)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = "Share meeting code",
                            modifier = Modifier
                                .size(24.dp),
                            tint = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                ) {
                    Column {
                        Text(
                            text = "Meeting Code",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = meetingCode,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (joining) {
                CircularProgressIndicator()
            }

            Button(
                onClick = {
                    joining = true
                    coroutineScope.launch {
                        if(!onJoinClick()) joining = false
                        else{
                            viewModel.toggleCamera(isCamEnabled)
                            viewModel.toggleMic(isMicEnabled)
                            viewModel.toggleSpeakerAudio(!isMuted)
                        }
                    }
                },
                enabled = !joining,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                val buttonText = if (!joining) {
                    if (isAskToJoin) "Ask to Join" else "Join"
                }else if (isAskToJoin) "Asking to Join" else "Joining..."

                Icon(
                    painter = painterResource(id = R.drawable.video_camera),
                    contentDescription = "Join Meeting",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Joining as")
            // User details
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "User Photo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(id = R.drawable.baseline_account_circle)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CameraPreviewView(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}