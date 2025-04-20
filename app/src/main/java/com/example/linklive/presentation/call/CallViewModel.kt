package com.example.linklive.presentation.call

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.linklive.presentation.model.Participant
import androidx.compose.runtime.State

class CallViewModel : ViewModel() {
    private val _participants = mutableStateOf<List<Participant>>(emptyList())
    val participants: State<List<Participant>> = _participants

    // Socket client reference (initialize this in your ViewModel setup)
//    private lateinit var socketClient: SocketClient

    init {
        // Connect to socket and set up listeners
        setupSocketConnection()
    }

    private fun setupSocketConnection() {
        // Initialize socket client
        // socketClient = YourSocketClient()

        // Listen for audio/video toggle events
//        socketClient.on("participant_audio_toggled") { data ->
//            val participantId = data.getString("participantId")
//            val enabled = data.getBoolean("enabled")
//            updateParticipantAudio(participantId, enabled)
//        }
//
//        socketClient.on("participant_video_toggled") { data ->
//            val participantId = data.getString("participantId")
//            val enabled = data.getBoolean("enabled")
//            updateParticipantVideo(participantId, enabled)
//        }
    }

    // Helper to update a specific participant
    private fun updateParticipant(id: String, transform: (Participant) -> Participant) {
        _participants.value = _participants.value.map {
            if (it.id == id) transform(it) else it
        }
    }
}