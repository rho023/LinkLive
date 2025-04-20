package com.example.linklive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.linklive.presentation.navigation.AppNavGraph
import com.example.linklive.ui.theme.LinkLiveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkLiveTheme {
                AppNavGraph(isLoggedIn = true)
            }
        }
    }
}