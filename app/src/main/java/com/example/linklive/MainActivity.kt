package com.example.linklive

import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.linklive.presentation.call.CallViewModel
import com.example.linklive.presentation.navigation.AppNavGraph
import com.example.linklive.service.CallService
import com.example.linklive.ui.theme.LinkLiveTheme
import com.example.linklive.utils.PermissionUtil
import org.webrtc.PeerConnectionFactory

class MainActivity : ComponentActivity() {
    private val callViewModel: CallViewModel by viewModels()
    private lateinit var requestPermissionsLauncher : ActivityResultLauncher<Array<String>>
    private val REQUEST_CODE_SCREEN_CAPTURE = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("rho", "main me hu 1")

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Handle the result of the permission request
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                // Proceed with functionality
            } else {
                Toast.makeText(this, "Permissions required for audio and video are not granted.", Toast.LENGTH_SHORT).show()
            }
        }

        // Use PermissionUtil to check and request permissions
        val permissionUtil = PermissionUtil(this, requestPermissionsLauncher)
        permissionUtil.checkAndRequestPermissions {
            // Callback when all permissions are granted
            Toast.makeText(this, "Permissions granted! Proceeding...", Toast.LENGTH_SHORT).show()
        }

        enableEdgeToEdge()
        setContent {
            LinkLiveTheme {
                AppNavGraph(
                    callViewModel = callViewModel,
                    startService = {
                        initialize()

                        // Start service and bind to it
                        val roomId = intent.getStringExtra("roomId") ?: "023"
                        callViewModel.setRoomId(roomId)

                        startCallService()
                    },
                    isLoggedIn = true
                )
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("rho", "main me hu 2")

        if(requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Pass the projection data to your service
            val serviceIntent = Intent(this, CallService::class.java)
            val roomId = "023"
            serviceIntent.putExtra("roomId", roomId)
            serviceIntent.putExtra("resultCode", resultCode)
            serviceIntent.putExtra("data", data)

            ContextCompat.startForegroundService(this, serviceIntent)
            callViewModel.bindToService(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume participants data when activity comes to foreground
    }

    override fun onDestroy() {
        callViewModel.unbindFromService(this)
        super.onDestroy()
    }

    fun initialize() {
        // Call this from Application.onCreate()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )
    }

    private fun startCallService() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }
}