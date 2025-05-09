package com.example.linklive.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionUtil(
    private val context: Context,
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
) {
    fun checkAndRequestPermissions(onPermissionsGranted: () -> Unit) {
        // Permissions required
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )

        // Filter permissions that are not granted
        val notGrantedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isEmpty()) {
            // All permissions are granted
            onPermissionsGranted()
        } else {
            // Request permissions
            requestPermissionsLauncher.launch(notGrantedPermissions.toTypedArray())
        }
    }
}