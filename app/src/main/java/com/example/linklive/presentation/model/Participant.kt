package com.example.linklive.presentation.model

import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class Participant(
    val id: String,
    val name: String,
    val photoUrl: String,
    val audioTrack: AudioTrack? = null,
    val videoTrack: VideoTrack? = null
)