package com.example.linklive.data.socket

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.example.linklive.data.model.Participant
import com.example.linklive.data.preferences.getUserInfo
import com.example.linklive.domain.ServiceCallbackInterface
import com.example.linklive.utils.CameraCapturerFactory
import com.example.linklive.utils.JsonUtils
import com.example.linklive.utils.PeerConnectionUtils
import com.example.linklive.utils.toJsonObject
import io.github.crow_misia.mediasoup.Consumer
import io.github.crow_misia.mediasoup.Device
import io.github.crow_misia.mediasoup.Producer
import io.github.crow_misia.mediasoup.RecvTransport
import io.github.crow_misia.mediasoup.SendTransport
import io.github.crow_misia.mediasoup.Transport
import io.github.crow_misia.mediasoup.createDevice
import io.github.crow_misia.webrtc.RTCComponentFactory
import io.github.crow_misia.webrtc.RTCLocalAudioManager
import io.github.crow_misia.webrtc.RTCLocalVideoManager
import io.github.crow_misia.webrtc.option.MediaConstraintsOption
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoTrack
import java.net.URISyntaxException
import kotlin.coroutines.resumeWithException
import kotlin.text.ifEmpty

@OptIn(ExperimentalCoroutinesApi::class)
class SocketHandler(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val serviceCallback: ServiceCallbackInterface
) {

    companion object {
        private const val TAG = "Socket"
    }

    private lateinit var socket: Socket
    private var micProducer: Producer? = null
    private var camProducer: Producer? = null
    private var sendTransport: SendTransport? = null
    private var recvTransport: RecvTransport? = null
    lateinit var mediaSoupDevice : Device
    private lateinit var localAudioManager : RTCLocalAudioManager
    private lateinit var localVideoManager : RTCLocalVideoManager
    private lateinit var componentFactory: RTCComponentFactory
    private var cameraName = "front"
    private var isProduce = true
    private var isConsume = true
    private var isUseDataChannel = true

    private lateinit var camCapturer: CameraVideoCapturer
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var mediaConstraintsOption: MediaConstraintsOption
    private lateinit var nProducers: String
    private var myPeerId = ""
    private val rtcConfig = PeerConnection.RTCConfiguration(emptyList())

    private lateinit var videoProducerId: String
    private lateinit var audioProducerId: String
    private var isSpeakerAudio = false

    fun toggleSpeakerAudio(isEnabled: Boolean) {
        isSpeakerAudio = isEnabled
    }

    fun toggleCamera(isEnabled: Boolean) {
        coroutineScope.launch {
            if(isEnabled) {
                serviceCallback.toggleParticipantVideo(myPeerId, true)
                resumeVideo()
            } else {
                serviceCallback.toggleParticipantVideo(myPeerId, false)
                pauseVideo()
            }
        }
    }

    fun toggleMic(isEnabled: Boolean) {
        coroutineScope.launch {
            if(isEnabled) {
                serviceCallback.toggleParticipantAudio(myPeerId, true)
                resumeAudio()
            } else {
                serviceCallback.toggleParticipantAudio(myPeerId, false)
                pauseAudio()
            }
        }
    }

    suspend fun setupSocketConnection(
        roomId: String,
        peerId: String,
        isHost: Boolean
    ): Boolean {
        try {
            myPeerId = peerId
            initializeSocket()
            val roomData = joinRoom(roomId, isHost)
            if(roomData.has("approved")) return false
            initializeMediaComponents()
            setupMediaSoupDevice(roomData)
            createTransports()
            removeParticipant()
            enableMediaIfPossible()
            consumeRemoteProducers()
            consumeNewProducer()
            pauseProducerOfPeer()
            resumeProducerOfPeer()
            receiveMessages()
            if(isHost) handlePeerRequest()
        } catch (e: Exception) {
            Log.e(TAG, "Setup failed", e)
            return false
            //Proper error handling - notify UI, etc.
        }
        return true
    }

    private fun initializeSocket() {
        try {
            socket = IO.socket("http://192.168.1.5:3000")
            socket.connect()
            Log.d(TAG, "Socket connected: ${socket.connected()}")

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("socket", "Connection error: ${args[0]}")
            }
            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("socket", "Socket disconnected")
            }
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL: ", e)
            // Notify the UI or retry connection
        }
    }

    private suspend fun joinRoom(roomId: String, isHost: Boolean): JSONObject {
        val roomRequest = JSONObject()
            .put("roomId", roomId)
            .put("peerId", myPeerId)
            .put("isHost", isHost)
        socket.emit("join-room", roomRequest)
        Log.d(TAG, "Socket join-room: $roomRequest")
        val approved = socket.awaitEvent("join-approved")
        Log.d(TAG, "approved: ${approved.getBoolean("approved")}")
        return if(approved.getBoolean("approved")) socket.awaitEvent("room-joined") else approved
    }

    private fun initializeMediaComponents() {
        //Camera setup
        camCapturer = CameraCapturerFactory.create(
            context,
            fixedResolution = false,
            preferenceFrontCamera = "front" == cameraName
        )!!

        //Create media constraints
        mediaConstraintsOption = MediaConstraintsOption().also {
            it.enableAudioDownstream()
            it.enableAudioUpstream()
            it.videoEncoderFactory = DefaultVideoEncoderFactory(
                PeerConnectionUtils.eglContext,
                true,
                true
            )
            it.videoDecoderFactory = DefaultVideoDecoderFactory(
                PeerConnectionUtils.eglContext
            )
            it.enableVideoDownstream(PeerConnectionUtils.eglContext)
            it.audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            camCapturer.also { capturer ->
                it.enableVideoUpstream(capturer, PeerConnectionUtils.eglContext)
            }
        }

        componentFactory = RTCComponentFactory(mediaConstraintsOption)
        peerConnectionFactory = componentFactory.createPeerConnectionFactory(context) { _, _ -> }
        localAudioManager = componentFactory.createAudioManager()!!
        localVideoManager = componentFactory.createVideoManager()!!
        mediaSoupDevice = peerConnectionFactory.createDevice()
    }

    private suspend fun setupMediaSoupDevice(roomData: JSONObject) {
        val routerRtpCapabilities = roomData.getJSONObject("routerRtpCapabilities").toString()
        val producers = roomData.getJSONArray("producers")
        Log.d(TAG, "myPeerId: $myPeerId")
        coroutineScope.launch(Dispatchers.Main) {
            serviceCallback.addMyParticipant(
                Participant(id = myPeerId, name = "You", photoUrl = getUserInfo(context).second)
            )
        }
        nProducers = producers.toString()
        val peers = roomData.getJSONObject("peers")
        val jsonArray = JSONArray()
        peers.keys().forEach { key ->
            if (peers.getBoolean(key.toString())) {
                val jsonObject = JSONObject().put("key", key).put("value", true)
                jsonArray.put(jsonObject)
            }
        }
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i) // Get each JSONObject
            val peerId = jsonObject.getString("key") // Extract the "key" field as the peer ID

            if(peerId != myPeerId) {
                Log.d(TAG, "Peer ID: $peerId")
                // Process the peer information
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.addParticipant(
                        Participant(id = peerId, name = "Peer $i")
                    )
                }
            }
        }

        withContext(Dispatchers.Default) {
            if (!mediaSoupDevice.loaded) {
                mediaSoupDevice.load(routerRtpCapabilities, rtcConfig)
            }
        }
    }

    private suspend fun createTransports() = withContext(Dispatchers.IO) {
        val sctpCapabilities = if (isUseDataChannel) {
            mediaSoupDevice.sctpCapabilities.toJsonObject()
        } else null

        createSendTransport(sctpCapabilities)
        createRecvTransport(sctpCapabilities)
    }

    private suspend fun createSendTransport(sctpCapabilities: JSONObject?) {
        try {
            //Request transport info from server
            val transportInfo = socket.emitAndAwait("create-send-transport")
            JsonUtils.jsonPut(transportInfo, "sctpCapabilities", sctpCapabilities)

            //Extract parameters
            val id = transportInfo.getString("id")
            val iceParameters = transportInfo.getJSONObject("iceParameters").toString()
            val iceCandidates = transportInfo.getJSONArray("iceCandidates").toString()
            val dtlsParameters = transportInfo.getJSONObject("dtlsParameters")
            val sctpParameters = transportInfo.optString("sctpParameters").ifEmpty { null }

            //Create transport
            sendTransport = mediaSoupDevice.createSendTransport(
                listener = sendTransportListener,
                id = id,
                iceParameters = iceParameters,
                iceCandidates = iceCandidates,
                dtlsParameters = dtlsParameters.toString(),
                sctpParameters = sctpParameters,
                appData = null,
                rtcConfig = rtcConfig
            )

            Log.d(TAG, "Send transport created: ${sendTransport?.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating send transport", e)
            throw e
        }
    }

    private suspend fun createRecvTransport(sctpCapabilities: JSONObject?) {
        try {
            val transportInfo = socket.emitAndAwait("create-recv-transport")
            JsonUtils.jsonPut(transportInfo, "sctpCapabilities", sctpCapabilities)

            //Extract parameters (similar to createSendTransport)
            val id = transportInfo.getString("id")
            val iceParameters = transportInfo.getJSONObject("iceParameters").toString()
            val iceCandidates = transportInfo.getJSONArray("iceCandidates").toString()
            val dtlsParameters = transportInfo.getJSONObject("dtlsParameters")
            val sctpParameters = transportInfo.optString("sctpParameters").ifEmpty { null }

            //Create transport
            recvTransport = mediaSoupDevice.createRecvTransport(
                listener = recvTransportListener,
                id = id,
                iceParameters = iceParameters,
                iceCandidates = iceCandidates,
                dtlsParameters = dtlsParameters.toString(),
                sctpParameters = sctpParameters,
                appData = null,
                rtcConfig = rtcConfig
            )

            Log.d(TAG, "Receive transport created: ${recvTransport?.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating receive transport", e)
            throw e
        }
    }

    private suspend fun enableMediaIfPossible() {
        if(isProduce && mediaSoupDevice.loaded) {
            val canSendMic = mediaSoupDevice.canProduce("audio")
            val canSendCam = mediaSoupDevice.canProduce("video")

            if(canSendMic) enableMic()
            if(canSendCam) enableCam()
        }
    }

    private suspend fun enableMic() {
        micProducer?.close()
        localAudioManager.dispose()
        micProducer = null
        //Implementation for enabling microphone
        localAudioManager.initTrack(peerConnectionFactory, mediaConstraintsOption)
        val track = localAudioManager.track ?: run {
            Log.d(TAG, "audio track null")
            return
        }
        withContext(Dispatchers.Main) {
            serviceCallback.updateParticipantAudio(myPeerId, track)
        }
        track.setEnabled(true)
        val micProducer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    Log.d("socket", "micProducer = $micProducer")
                    micProducer?.also {
                        Log.d("socket", "removeProducer = ${it.id}")
                        localAudioManager.dispose()
                        micProducer = null
                    }
                }
            },
            track = track,
            encodings = emptyList(),
            codecOptions = null,
            appData = null
        )
        this.micProducer = micProducer
        Log.d(TAG, "addProducer = $micProducer")
    }

    private suspend fun enableCam() {
        camProducer?.close()
        camProducer = null
        localVideoManager.dispose()
        camCapturer.stopCapture()
        //Implementation for enabling camera
        localVideoManager.initTrack(peerConnectionFactory, mediaConstraintsOption, context)
        camCapturer.startCapture(640, 480, 30)
        val track = localVideoManager.track ?: run {
            Log.d(TAG, "video track null")
            return
        }
        track.setEnabled(true)
        withContext(Dispatchers.Main) {
            serviceCallback.updateParticipantVideo(myPeerId, track)
        }
        val camProducer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    Log.d(TAG, "onTransportClose(), camProducer")
                    camProducer?.also {
                        localVideoManager.dispose()
                        camCapturer.stopCapture()
                        camProducer = null
                    }
                }
            },
            track = track,
            encodings = emptyList(),
            codecOptions = null,
            appData = null
        )
        this.camProducer = camProducer
        Log.d(TAG, "addProducer = $camProducer")
    }

    fun switchCamera() {
        if(::camCapturer.isInitialized) {
            camCapturer.switchCamera(null)
            cameraName = if(cameraName == "front") "back" else "front"
            Log.d(TAG, "switchCamera: Camera switched to $cameraName")
        } else {
            Log.e(TAG, "Camera capturer not initialized")
        }
    }

    private val sendTransportListener = object : SendTransport.Listener {
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            Log.d(TAG, "onConnect - send transport")
            val jsonParameters = JSONObject(dtlsParameters)
            val payload = JSONObject().put("dtlsParameters", jsonParameters)

            coroutineScope.launch {
                try {
                    socket.emitAndAwait("connect-send-transport", payload)
                    Log.d(TAG, "Send transport connected")
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting send transport", e)
                }
            }
        }

        override fun onConnectionStateChange(transport: Transport, newState: String) {
            Log.d(TAG, "Send transport connection state changed: $newState")
        }

        override fun onProduce(
            transport: Transport,
            kind: String,
            rtpParameters: String,
            appData: String?
        ): String {
            Log.d(TAG, "onProduce() $kind")
            val json = JSONObject().apply {
                put("transportId", transport.id)
                put("kind", kind)
                put("rtpParameters", rtpParameters.toJsonObject())
            }

            //We need to use runBlocking here because this interface function must return synchronously
            return runBlocking {
                try {
                    val response = socket.emitAndAwait("produce", json)
                    val producerId = response.optString("id", "")
                    if(kind == "audio") audioProducerId = producerId else videoProducerId = producerId
                    producerId
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to produce", e)
                    ""
                }
            }
        }

        override fun onProduceData(
            transport: Transport,
            sctpStreamParameters: String,
            label: String,
            protocol: String,
            appData: String?
        ): String {
            TODO("Not yet implemented")
        }
    }

    private val recvTransportListener = object : RecvTransport.Listener {
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            Log.d(TAG, "onConnect - receive transport")
            val jsonParameters = JSONObject(dtlsParameters)
            val payload = JSONObject().put("dtlsParameters", jsonParameters)

            coroutineScope.launch {
                try {
                    socket.emitAndAwait("connect-recv-transport", payload)
                    Log.d(TAG, "Receive transport connected")
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting receive transport", e)
                }
            }
        }

        override fun onConnectionStateChange(transport: Transport, newState: String) {
            Log.d(TAG, "Receive transport connection state: $newState")
        }
    }

    suspend fun Socket.emitAndAwait(event: String, data: Any? = null): JSONObject {
        Log.d(TAG, "emitAndAwait: $event")
        return suspendCancellableCoroutine { continuation ->
            val ackCallback = object : Ack {
                override fun call(vararg args: Any?) {
                    try {
                        if (args.isNotEmpty() && args[0] is JSONObject) {
                            continuation.resume(args[0] as JSONObject) {}
                        } else {
                            // Create a default success response if server doesn't return a JSONObject
                            continuation.resume(JSONObject().put("success", true)) {}
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            Exception("Invalid response from $event event, error message: ${e.message}")
                        )
                    }
                }
            }

            // Handle differently based on whether data is null
            if (data == null) {
                emit(event, ackCallback)
            } else {
                emit(event, data, ackCallback)
            }
        }
    }

    suspend fun Socket.awaitEvent(event: String): JSONObject {
        return suspendCancellableCoroutine { continuation ->
            once(event) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    continuation.resume(args[0] as JSONObject) {}
                    Log.d(TAG, "event: $event")
                } else {
                    continuation.resumeWithException(
                        Exception("Invalid data for $event event")
                    )
                }
            }
        }
    }

    private suspend fun consumeRemoteProducers() {
        try {
            Log.d(TAG, "nProducers: $nProducers")
            val producerJsonArray = JSONArray(nProducers)

            for(i in 0 until producerJsonArray.length()) {
                val producerJson = producerJsonArray.getJSONObject(i)
                val producerId = producerJson.getString("producerId")
                consumeMedia(producerId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming producers", e)
        }
    }

    private fun consumeNewProducer() {
        try {
            socket.on("new-producer") { args ->
                val newProducer = args[0] as JSONObject
                val newPeerId = newProducer.getString("peerId")

                if(newPeerId != myPeerId + "share") {
                    coroutineScope.launch(Dispatchers.Main) {
                        serviceCallback.addParticipant(
                            Participant(
                                id = newPeerId,
                                name = "New Peer"
                            )
                        )
                        Log.d(TAG, "New producer: $newProducer")
                        consumeMedia(newProducer.getString("producerId"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming new producer", e)
        }
    }

    private suspend fun consumeMedia(producerId: String) {
        val rtpCapabilities = mediaSoupDevice.rtpCapabilities
        val requestData = JSONObject().apply {
            put("producerId", producerId)
            put("rtpCapabilities", JSONObject(rtpCapabilities))
        }
        Log.d(TAG, "Consume request: $requestData")

        try {
            val responseData = socket.emitAndAwait("consume", requestData)

            val peerId = responseData.optString("peerId")
            val id = responseData.optString("id") ?: ""
            val kind = responseData.optString("kind")
            val rtpParameters = responseData.optString("rtpParameters")

            val consumer = recvTransport?.consume(
                listener = object : Consumer.Listener {
                    override fun onTransportClose(consumer: Consumer) {
                        Log.d(TAG, "onTransportClose for consumer: ${consumer.id}")
                    }
                },
                id = id,
                producerId = producerId,
                kind = kind,
                rtpParameters = rtpParameters,
                appData = null
            ) ?: return

            if("video" == consumer.kind) {
                val videoTrack = consumer.track as VideoTrack
                withContext(Dispatchers.Main) {
                    videoTrack.let {
                        serviceCallback.updateParticipantVideo(peerId, it)
                    }
                    serviceCallback.updateVideoConsumer(id)
                }
            } else {
                val audioTrack = consumer.track as AudioTrack
                withContext(Dispatchers.Main) {
                    audioTrack.setEnabled(isSpeakerAudio)
                    audioTrack.let {
                        serviceCallback.updateParticipantAudio(peerId, it)
                    }
                    serviceCallback.updateAudioConsumer(id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming media: ${e.message}")
        }
    }

    private fun pauseVideo() {
        val producerId = JSONObject().put("producerId", videoProducerId)
        socket.emit("pause-producer", producerId)
    }

    private fun resumeVideo() {
        val producerId = JSONObject().put("producerId", videoProducerId)
        socket.emit("resume-producer", producerId)
    }

    private fun pauseAudio() {
        val producerId = JSONObject().put("producerId", audioProducerId)
        socket.emit("pause-producer", producerId)
    }

    private fun resumeAudio() {
        val producerId = JSONObject().put("producerId", audioProducerId)
        socket.emit("resume-producer", producerId)
    }

    private fun pauseProducerOfPeer() {
        try {
            socket.on("producer-paused") { args ->
                val json = args[0] as JSONObject
                val peerId = json.getString("peerId")
                val kind = json.getString("kind")
                coroutineScope.launch(Dispatchers.Main) {
                    if(kind == "audio") {
                        val data = JSONObject().put("consumerId", serviceCallback.getAudioConsumer(peerId))
                        socket.emit("pause-consumer", data)
                        Log.d(TAG, "Consumer paused $data")
                        serviceCallback.toggleParticipantAudio(peerId, false)
                    } else {
                        val data = JSONObject().put("consumerId", serviceCallback.getVideoConsumer(peerId))
                        socket.emit("pause-consumer", data)
                        serviceCallback.toggleParticipantVideo(peerId, false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing producer of peer: ${e.message}")
        }
    }

    private fun resumeProducerOfPeer() {
        try {
            socket.on("producer-resumed") { args ->
                val json = args[0] as JSONObject
                val peerId = json.getString("peerId")
                val kind = json.getString("kind")
                coroutineScope.launch(Dispatchers.Main) {
                    if(kind == "audio") {
                        val data = JSONObject().put("consumerId", serviceCallback.getAudioConsumer(peerId))
                        socket.emit("resume-consumer", data)
                        Log.d(TAG, "Consumer resumed $data")
                        serviceCallback.toggleParticipantAudio(peerId, true)
                    } else {
                        val data = JSONObject().put("consumerId", serviceCallback.getVideoConsumer(peerId))
                        socket.emit("resume-consumer", data)
                        serviceCallback.toggleParticipantVideo(peerId, true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming producer of peer: ${e.message}")
        }
    }

    fun sendMessage(message: String) {
        val message = JSONObject().put("text", message)
        try {
            socket.emit("message", message)
            Log.d(TAG, "Message: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
        }
    }

    private fun receiveMessages() {
        try {
            socket.on("receive-message") { args ->
                val data = args[0] as JSONObject
                val senderPeerId = data.getString("peerId")
                val message = data.getString("text")
                Log.d(TAG, "Message received: $message")
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.addMessage(message, senderPeerId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving messages: ${e.message}")
        }
    }

    private fun removeParticipant() {
        try{
            socket.on("peer-disconnected") { args ->
                val data = args[0] as JSONObject
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.removeParticipant(data.getString("peerId"))
                }
            }
        } catch (e: Exception) {
            Log.e("socket", "Error getting disconnected peer", e)
        }
    }

    private fun handlePeerRequest() {
        Log.d(TAG, "ask")
        socket.on("ask-to-join") { args ->
            try {
                Log.d(TAG, "asking to join")
                if(args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val requesterPeerId = data.getString("requesterPeerId")
                    val requesterSocketId = data.getString("requesterSocketId")
                    serviceCallback.handlePeerRequest(requesterPeerId, requesterSocketId)
                    Log.d(TAG, "handlePeerRequest: $requesterPeerId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling ask-to-join: ${e.localizedMessage}", e)
            }
        }
    }

    fun approvePeer(approved: Boolean, requesterSocketId: String) {
        val response = JSONObject().apply {
            put("approved", approved)
            put("to", requesterSocketId)
        }
        Log.d(TAG, "approvePeer: $approved")
        socket.emit("ask-to-join-response", response)
    }

    fun closeConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendTransport?.close()
                recvTransport?.close()
                socket.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call: ${e.message}")
            } finally {
                sendTransport = null
                recvTransport = null
                socket.close()
                Log.d(TAG, "Socket closed completely")
            }
        }
    }
}