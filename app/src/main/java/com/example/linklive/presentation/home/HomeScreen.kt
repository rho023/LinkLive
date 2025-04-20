package com.example.linklive.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToJoinRoom: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {},
                modifier = Modifier
            ) {
                Text(
                    text = "Create Room"
                )
            }

            Button(
                onClick = { onNavigateToJoinRoom() },
                modifier = Modifier
            ) {
                Text(
                    text = "Join Room"
                )
            }
        }

    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onNavigateToJoinRoom = {})
}

