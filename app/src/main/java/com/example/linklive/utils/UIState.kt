package com.example.linklive.utils

sealed class UIState {
    object Idle : UIState()
    object Loading : UIState()
    data class Success(val message: String) : UIState()
    data class Error(val exception: Throwable) : UIState()
}