package com.example.linklive.presentation.call

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.linklive.R
import com.example.linklive.presentation.call.viewmodel.CallViewModel

@Composable
fun ParticipantListScreen(
    viewModel: CallViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val participants by viewModel.participants.collectAsState()
    val meetingCode by viewModel.roomId

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = meetingCode,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 18.sp
            )
        }

        // Share joining info
        Button(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, meetingCode)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Share joining information",
                color = Color.White
            )
        }

        // Participant List
        LazyColumn {
            items(participants.values.toList()) { participant ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = participant.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        placeholder = painterResource(id = R.drawable.baseline_account_circle)
                    )

                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)) {
                        Text(text = participant.name, style = MaterialTheme.typography.bodyLarge)
                    }

                    Icon(
                        painter = if (participant.isMute) painterResource(R.drawable.mute) else painterResource(R.drawable.mic),
                        contentDescription = null,
                        tint = if (!participant.isMute) Color.Green else Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Icon(
                        painter = if (participant.isVideoPaused) painterResource(R.drawable.video_camera_prohibited) else painterResource(R.drawable.video_camera),
                        contentDescription = null,
                        tint = if (!participant.isVideoPaused) Color.Green else Color.Gray
                    )
                }
            }
        }
    }
}