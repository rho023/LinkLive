package com.example.linklive.presentation.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linklive.presentation.model.Participant
import com.example.linklive.service.CallService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.jvm.java

class CallViewModel : ViewModel() {
    private val _participants = mutableStateOf<List<Participant>>(emptyList())
    val participants: State<List<Participant>> = _participants

    private val _roomId = mutableStateOf("")
    val roomId: State<String> = _roomId

    // Service connection
    private var _callServiceRef: WeakReference<CallService>? = WeakReference(null)
    private val callService: CallService?
        get() = _callServiceRef?.get()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CallService.VideoCallBinder
            val callService = binder.getService()
            _callServiceRef = WeakReference(callService)

            // Start collecting the participants from the service
            viewModelScope.launch {
                callService.participants.collectLatest { serviceParticipants ->
                    Log.d("CallViewModel", "Received participants: $serviceParticipants")
                    _participants.value = serviceParticipants
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _callServiceRef = WeakReference(null)
        }
    }

    fun bindToService(context: Context) {
        val intent = Intent(context, CallService::class.java)
        Log.d("rho", "Binding to service: ${participants.value}")
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindFromService(context: Context) {
        try {
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Service not bound, ignore
        }
    }

    fun setRoomId(roomId: String) {
        _roomId.value = roomId
    }

    fun endCall() {
        callService?.closeConnection()
        _participants.value = emptyList() // Clear participants
        _roomId.value = "" // Reset room ID
    }

    override fun onCleared() {
        super.onCleared()
        // Don't call closeConnection here as the service should keep running
    }
}