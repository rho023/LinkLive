package com.example.linklive.domain

import com.example.linklive.presentation.model.Participant
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

interface ServiceCallbackInterface {
    fun addParticipant(participant: Participant)
    fun updateParticipantVideo(id: String, videoTrack: VideoTrack?)
    fun updateParticipantAudio(id: String, audioTrack: AudioTrack?)
    fun removeParticipant(id: String)
}