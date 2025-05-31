package com.example.linklive.data.model

import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class Participant(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val audioTrack: AudioTrack? = null,
    val videoTrack: VideoTrack? = null,
    val videoConsumerId: String? = null,
    val audioConsumerId: String? = null,
    var isMute: Boolean = false,
    var isVideoPaused: Boolean = false
)