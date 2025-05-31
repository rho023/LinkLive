package com.example.linklive

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.linklive.data.preferences.getPeerId
import com.example.linklive.data.preferences.isUserLoggedIn
import com.example.linklive.presentation.call.viewmodel.CallViewModel
import com.example.linklive.presentation.navigation.AppNavGraph
import com.example.linklive.data.service.CallService
import com.example.linklive.data.service.ShareScreenService
import com.example.linklive.ui.theme.LinkLiveTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.PeerConnectionFactory

class MainActivity : ComponentActivity() {
    private val callViewModel: CallViewModel by viewModels()
    private val REQUEST_CODE_SCREEN_CAPTURE = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = runBlocking { isUserLoggedIn(this@MainActivity) }
        val peerId = runBlocking { getPeerId(this@MainActivity) }

        Log.d("socket", "peerId: $peerId, isLoggedIn: $isLoggedIn")

//        enableEdgeToEdge()
        setContent {
            LinkLiveTheme {
                AppNavGraph(
                    isLoggedIn = isLoggedIn,
                    peerId = peerId.toString(),
                    callViewModel = callViewModel,
                    startService = { roomId, peerId, isHost ->
                        initialize()
                        return@AppNavGraph startCallService(roomId, peerId, isHost)
                    },
                    onEndCall = { callViewModel.endCall(this) },
                    startScreenSharing = { startScreenSharing() },
                    stopScreenSharing = { stopScreenSharing() }
                )
            }
        }
    }

    private suspend fun startCallService(roomId: String, peerId: String, isHost: Boolean): Boolean {
        val serviceIntent = Intent(this, CallService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        callViewModel.bindToService(this)
        callViewModel.setServiceStartedValue(true)
        return callViewModel.initializeSocket(roomId, peerId, isHost)
    }

    private fun startScreenSharing() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    private fun stopScreenSharing() {
        callViewModel.stopScreenSharing()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Pass the projection data to your service
            val serviceIntent = Intent(this, ShareScreenService::class.java)
            val roomId = intent.getStringExtra("roomId") ?: "023"
            serviceIntent.putExtra("roomId", roomId)
            ContextCompat.startForegroundService(this, serviceIntent)
            lifecycleScope.launch {
                callViewModel.bindToShareScreenAwait(this@MainActivity)
                callViewModel.startScreenSharing(resultCode, data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipManually() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d("pip", "onPictureInPictureModeChanged: $isInPictureInPictureMode")
        callViewModel.isPipMode.value = isInPictureInPictureMode
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        if(callViewModel.isServiceStarted) {
            super.onUserLeaveHint()
            enterPipManually()
        }
    }
}