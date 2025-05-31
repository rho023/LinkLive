package com.example.linklive.data.model

import kotlinx.coroutines.CompletableDeferred

data class JoinRequest(
    val peerId: String,
    val socketId: String,
    val result: CompletableDeferred<Boolean>
)
