package com.example.linklive.presentation.home

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.linklive.R
import com.example.linklive.data.preferences.getUserInfo
import com.example.linklive.utils.SetStatusBarStyle

@Composable
fun HomeScreen(
    navController: NavController
) {
    PermissionRequester()
    val context = LocalContext.current
    var joinCode by rememberSaveable { mutableStateOf("") }
    var userName by rememberSaveable { mutableStateOf("") }
    var userPhotoUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val userInfo = getUserInfo(context)
        userName = userInfo.first.toString()
        userPhotoUrl = userInfo.second.toString()
    }

    SetStatusBarStyle(true)
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 15.dp, top = 50.dp)
        ) {
            Text(
                text = "Welcome, $userName",
                color = Color.Black,
                fontSize = 27.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = userPhotoUrl,
                contentDescription = "User Photo",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                placeholder = painterResource(id = R.drawable.baseline_account_circle),
                error = painterResource(id = R.drawable.baseline_account_circle),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 15.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Button(
                onClick = {
                    val meetingCode = Uri.encode(generalMeetingCode())
                    val name = Uri.encode(userName)
                    val photoUrl = Uri.encode(userPhotoUrl)
                    val isAskToJoin = false.toString()
                    navController.navigate(
                        "meetingPreview/$meetingCode/$name/$photoUrl/$isAskToJoin"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.link),
                    contentDescription = "Create Link",
                    modifier = Modifier
                        .size(35.dp)
                        .padding(end = 8.dp),
                    tint = Color.White
                )
                Text(
                    text = "Create Link",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    placeholder = { Text("Enter a code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(50),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Gray,
                        unfocusedIndicatorColor = Color.Gray,
                        disabledIndicatorColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = Color.Black,
                        focusedTextColor = Color.Gray
                    )
                )

                TextButton (
                    onClick = {
                        if (joinCode.isNotBlank()) {
                            val meetingCode = Uri.encode(joinCode)
                            val name = Uri.encode(userName)
                            val photoUrl = Uri.encode(userPhotoUrl)
                            val isAskToJoin = true.toString()
                            navController.navigate("meetingPreview/$meetingCode/$name/$photoUrl/$isAskToJoin")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, disabledContainerColor = Color.LightGray),
                    enabled = joinCode.isNotBlank()
                ) {
                    Text(
                        text = "Join Link",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

fun generalMeetingCode(): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    return List(3) {
        (1..3).map { chars.random() }.joinToString("")
    }.joinToString("-")
}

@Composable
fun PermissionRequester() {
    val context = LocalContext.current

    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.all { it.value }
        if(allGranted) {

        } else {

        }
    }

    LaunchedEffect(Unit) {
        // Ask only if permissions are not granted
        if(permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {

        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(navController = NavController(LocalContext.current))
}

