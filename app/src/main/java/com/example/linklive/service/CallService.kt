package com.example.linklive.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.linklive.MainActivity
import com.example.linklive.R
import com.example.linklive.data.SocketHandler
import com.example.linklive.domain.ServiceCallbackInterface
import com.example.linklive.presentation.model.Participant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import java.lang.Exception

class CallService: Service() {

    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants.asStateFlow()

    private val binder = VideoCallBinder()
    private lateinit var socketHandler: SocketHandler

//    private var peerId: String? = null

    companion object {
        private const val CHANNEL_ID = "CallServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "CallService"
    }

    inner class VideoCallBinder : Binder() {
        fun getService(): CallService = this@CallService
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "call service called!")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create and display notification immediately
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("You are in a video call")
            .setContentText("Tap to return to the call")
            .setSmallIcon(R.drawable.ic_video_call)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        // Extract media projection data
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        // Initialize socket with room ID
        val roomId = intent?.getStringExtra("roomId") ?: "023"
        initializeSocketHandler(roomId)

        if(resultCode == Activity.RESULT_OK && data != null) {
            try {
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                // Store this for when you implement screen sharing
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get media projection: ${e.message}")
            }
        }

        return START_STICKY
    }

    private fun initializeSocketHandler(roomId: String) {
        socketHandler = SocketHandler(
            serviceCallback = createServiceCallback(),
            context = this,
            coroutineScope = CoroutineScope(Dispatchers.IO)
        )
        socketHandler.setupSocketConnection(roomId)
    }

    private fun createServiceCallback() = object : ServiceCallbackInterface {

        override fun addParticipant(participant: Participant) {
            _participants.update { currentList ->
                if(currentList.none { it.id == participant.id }) {
                    currentList + participant
                } else {
                    currentList
                }
            }
            Log.d(TAG, "Participant added: ${_participants.value}")
        }

        override fun updateParticipantAudio(id: String, audioTrack: AudioTrack?) {
            _participants.update { currentList ->
                currentList.map { participant ->
                    if(participant.id == id) participant.copy(audioTrack = audioTrack)
                    else participant
                }
            }
        }

        override fun removeParticipant(id: String) {
            _participants.update { currentList ->
                currentList.filter { participant ->
                    participant.id != id
                }
            }
        }

        override fun updateParticipantVideo(id: String, videoTrack: VideoTrack?) {
            _participants.update { currentList ->
                currentList.map { participant ->
                    if(participant.id == id) participant.copy(videoTrack = videoTrack)
                    else participant
                }
            }
        }
    }

    fun getParticipants(): List<Participant> = _participants.value

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        socketHandler.closeConnection()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ongoing call")
            .setContentText("Tap to return to your meeting")
            .setSmallIcon(R.drawable.ic_video_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Video Call Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun closeConnection() {
        // Clear participants list
        _participants.update {
            emptyList()
        }

        // Close socket connection
        socketHandler.closeConnection()

        // Stop the service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}