package com.example.linklive.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.linklive.R
import com.example.linklive.utils.SetStatusBarStyle
import com.example.linklive.utils.UIState

@Composable
fun AuthScreen(onNavigateToHome: () -> Unit) {
    val owner = LocalViewModelStoreOwner.current

    val viewModel: AuthViewModel = remember {
        ViewModelProvider(owner!!)[AuthViewModel::class.java]
    }

    val authState = viewModel.authState.collectAsState().value

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register

    SetStatusBarStyle(false)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(R.drawable.auth_background),
            contentDescription = null,
            contentScale = ContentScale.Companion.Crop,
            modifier = Modifier.Companion
                .fillMaxSize()
        )

        // Main Content
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.Companion.height(32.dp))

            Text(
                "Go ahead and set up your account",
                fontWeight = FontWeight.Companion.Bold,
                fontSize = 35.sp,
                lineHeight = 40.sp,
                color = Color.Companion.White,
                modifier = Modifier.Companion.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.Companion.height(16.dp))
            Text(
                "Sign in/up to enjoy the best video call experience",
                fontSize = 16.sp,
                color = Color.Companion.Gray,
                modifier = Modifier.Companion
                    .align(Alignment.Companion.Start)
                    .padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.Companion.height(24.dp))

            Card(
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 30.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Companion.White
                ),
                modifier = Modifier.Companion
                    .fillMaxWidth()
            ) {
                ToggleTabRow(selectedTab) {
                    selectedTab = it
                }

                when (selectedTab) {
                    0 -> LoginScreen()
                    1 -> RegisterScreen()
                }
            }
        }

        // UI State Handling
        when(authState) {
            is UIState.Idle -> {
                // Do nothing
            }
            is UIState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)), // Optional dim background
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UIState.Error -> {

            }
            is UIState.Success -> {
                onNavigateToHome()
            }
        }
    }
}

@Composable
fun ToggleTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.Companion
            .padding(16.dp)
            .height(52.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(Color(0xFFE0E0E0)) // background of the toggle group
            .fillMaxWidth()
    ) {
        val tabTitles = listOf("Login", "Register")
        tabTitles.forEachIndexed { index, title ->
            val isSelected = index == selectedTab
            val backgroundColor =
                if (isSelected) Color.Companion.White else Color.Companion.Transparent
            val textColor = if (isSelected) Color.Companion.Black else Color.Companion.DarkGray

            Surface(
                modifier = Modifier.Companion
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .clickable { onTabSelected(index) }
                    .align(Alignment.Companion.CenterVertically)
                    .weight(1f),
                color = backgroundColor
            ) {
                Box(
                    modifier = Modifier.Companion
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .align(Alignment.Companion.CenterVertically),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Text(
                        modifier = Modifier.Companion.height(20.dp),
                        text = title,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Companion.Medium
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun PreviewLogin() {
    AuthScreen(onNavigateToHome = {})
}