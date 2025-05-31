package com.example.linklive.presentation.call.viewmodel

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
import com.example.linklive.data.model.Message
import com.example.linklive.data.model.Participant
import com.example.linklive.data.service.CallService
import com.example.linklive.data.service.ShareScreenService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class CallViewModel : ViewModel() {
    private val _participants = MutableStateFlow<Map<String, Participant>>(emptyMap())
    val participants: StateFlow<Map<String, Participant>> = _participants.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _requestQueue = MutableStateFlow<ArrayDeque<Pair<String, String>>>(ArrayDeque())
    val requestQueue: StateFlow<ArrayDeque<Pair<String, String>>> = _requestQueue

    val roomId = mutableStateOf<String>("")
    val peerId = mutableStateOf<String>("")

    private val _focusedParticipant = mutableStateOf<Participant?>(null)
    val focusedParticipant: State<Participant?> = _focusedParticipant

    val isMicEnabled = mutableStateOf(true)
    val isCamEnabled = mutableStateOf(true)
    val isMuted = mutableStateOf(false)
    val isScreenSharing = mutableStateOf(false)

    val isPipMode = mutableStateOf(false)
    var isServiceStarted = false

    // Service connection
    private var _callServiceRef: WeakReference<CallService>? = WeakReference(null)
    private val callService: CallService?
        get() = _callServiceRef?.get()

    private var _screenShareScreenRef: WeakReference<ShareScreenService?> = WeakReference(null)
    private val screenShareService: ShareScreenService?
        get() = _screenShareScreenRef.get()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CallService.VideoCallBinder
            val callService = binder.getService()
            _callServiceRef = WeakReference(callService)
            callServiceReady.complete(Unit)
            Log.d("CallService", "service connected")

            // Start collecting the participants from the service
            viewModelScope.launch {
                callService.participants.collectLatest { serviceParticipants ->
                    _participants.value = serviceParticipants
                }
            }
            viewModelScope.launch {
                callService.messages.collectLatest { messages ->
                    _messages.value = messages
                    Log.d("CallService", " messagesV: ${_messages.value.size}")
                }
            }
            viewModelScope.launch {
                callService.focusedParticipant.collectLatest { serviceFocusedParticipant->
                    _focusedParticipant.value = serviceFocusedParticipant
                }
            }
            viewModelScope.launch {
                callService.requestQueue.collectLatest { serviceRequestQueue->
                    _requestQueue.value = serviceRequestQueue
                }
            }
            viewModelScope.launch {
                callService.isServiceStarted.collectLatest { serviceIsServiceStarted->
                    isServiceStarted = serviceIsServiceStarted
                }
            }
            viewModelScope.launch {
                callService.isMicEnabled.collectLatest { serviceIsMicEnabled->
                    isMicEnabled.value = serviceIsMicEnabled
                }
            }
            viewModelScope.launch {
                callService.isCamEnabled.collectLatest { serviceIsCamEnabled->
                    isCamEnabled.value = serviceIsCamEnabled
                }
            }
            viewModelScope.launch {
                callService.isMuted.collectLatest { serviceIsMuted ->
                    isMuted.value = serviceIsMuted
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _callServiceRef = WeakReference(null)
            callServiceReady = CompletableDeferred()
        }
    }

    private var screenSharingServiceReady = CompletableDeferred<Unit>()
    private var callServiceReady = CompletableDeferred<Unit>()

    private val screenShareServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShareScreenService.ShareScreenBinder
            _screenShareScreenRef = WeakReference(binder.getService())
            screenSharingServiceReady.complete(Unit)
            viewModelScope.launch {
                screenShareService?.isScreenSharing?.collectLatest { screenSharing ->
                    isScreenSharing.value = screenSharing
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _screenShareScreenRef = WeakReference(null)
            screenSharingServiceReady = CompletableDeferred()
        }
    }

    suspend fun bindToShareScreenAwait(context: Context) {
        Log.d("ScreenShare", "bindToShareService")
        screenSharingServiceReady = CompletableDeferred()
        val intent = Intent(context, ShareScreenService::class.java)
        context.bindService(intent, screenShareServiceConnection, Context.BIND_AUTO_CREATE)
        screenSharingServiceReady.await()
    }

    suspend fun bindToService(context: Context) {
        callServiceReady = CompletableDeferred()
        val intent = Intent(context, CallService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        callServiceReady.await()
    }

    suspend fun initializeSocket(roomId: String, peerId: String, isHost: Boolean): Boolean {
        return callService?.initializeSocketHandler(roomId, peerId, isHost) == true
    }

    fun setPeerId(peerId: String) {
        callService?.setPeerId(peerId)
        this.peerId.value = peerId
    }

    fun setRoomId(roomId: String) {
        callService?.setRoomId(roomId)
        this.roomId.value = roomId
    }

    fun startScreenSharing(resultCode: Int, data: Intent) {
        screenShareService?.startScreenSharing(resultCode, data, roomId.value, peerId.value)
    }

    fun stopScreenSharing() {
        screenShareService?.stopScreenSharing()
    }

    fun setFocusedParticipant(participant: Participant?) {
        _focusedParticipant.value = participant
        callService!!.setFocusedParticipant(participant)
    }

    fun approvePeer(approved: Boolean, socketId: String) {
        callService?.approvePeer(approved, socketId)
    }

    fun dequeRequest(): Pair<String, String>? {
        return callService?.dequeueRequest()
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun endCall(context: Context) {
        callService?.closeConnection()
        screenShareService?.stopScreenSharing()
        _participants.value = emptyMap()
        _focusedParticipant.value = null
        roomId.value = ""
        callService?.isServiceStarted?.value = false
        try {
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Service not bound, ignore
        }
    }

    fun switchCamera() {
        callService?.switchCamera()
    }

    fun toggleCamera(isEnabled: Boolean) {
        callService?.setCameraEnabled(isEnabled)
        isCamEnabled.value = isEnabled
    }

    fun toggleMic(isEnabled: Boolean) {
        callService?.setMicEnabled(isEnabled)
        isMicEnabled.value = isEnabled
    }

    fun sendMessage(message: String) {
        callService?.sendMessage(message)
    }
    fun toggleSpeakerAudio(isEnabled: Boolean) {
        callService?.toggleSpeakerAudio(isEnabled)
        isMuted.value = !isEnabled
    }
    fun setServiceStartedValue(value: Boolean) {
        isServiceStarted = true
        callService?.isServiceStarted?.value = value
    }
    suspend fun getDataFromFirestore(peerId: String): Map<String, Any>? {
        val userData = callService?.getUserDataFromFirestore(peerId)
        Log.d("socket", "firestore $userData")
        return userData
    }
}