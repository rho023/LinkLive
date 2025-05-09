package com.example.linklive.data

import android.content.Context
import android.util.Log
import io.github.crow_misia.mediasoup.Device
import io.github.crow_misia.mediasoup.Producer
import io.github.crow_misia.mediasoup.RecvTransport
import io.github.crow_misia.mediasoup.SendTransport
import io.github.crow_misia.webrtc.RTCComponentFactory
import io.github.crow_misia.webrtc.RTCLocalAudioManager
import io.github.crow_misia.webrtc.RTCLocalVideoManager
import com.example.linklive.utils.CameraCapturerFactory
import io.github.crow_misia.webrtc.option.MediaConstraintsOption
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.media.MediaRecorder.AudioSource
import com.example.linklive.domain.ServiceCallbackInterface
import com.example.linklive.presentation.model.Participant
import com.example.linklive.utils.JsonUtils
import com.example.linklive.utils.PeerConnectionUtils
import com.example.linklive.utils.toJsonObject
import io.github.crow_misia.mediasoup.Consumer
import io.github.crow_misia.mediasoup.Transport
import io.github.crow_misia.mediasoup.createDevice
import io.socket.client.Ack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoTrack
import kotlin.coroutines.resumeWithException

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
    private lateinit var myPeerId: String

    private val rtcConfig = PeerConnection.RTCConfiguration(emptyList())

    fun setupSocketConnection(roomId: String) = coroutineScope.launch {
        try {
            initializeSocket()
            val roomData = joinRoom(roomId)
            initializeMediaComponents()
            setupMediaSoupDevice(roomData)
            createTransports()
            enableMediaIfPossible()
            removeParticipant()
            consumeRemoteProducers()
            consumeNewProducer()
        } catch (e: Exception) {
            Log.e(TAG, "Setup failed", e)
            //Proper error handling - notify UI, etc.
        }
    }

    private fun initializeSocket() {
        try {
            socket = IO.socket("http://192.168.25.220:3000")
            socket.connect()
            Log.d(TAG, "Socket connected: ${socket.connected()}")

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("socket", "Connection error: ${args[0]}")
            }
            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("socket", "Socket disconnected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing socket: ${e.message}", e)
            // Notify the UI or retry connection
        }
    }

    private suspend fun joinRoom(roomId: String): JSONObject {
        val roomRequest = JSONObject().put("roomId", roomId)
        socket.emit("join-room", roomRequest)
        Log.d(TAG, "Socket join-room: $roomRequest")
        return socket.awaitEvent("room-joined")
    }

    private fun initializeMediaComponents() {
        //Camera setup
        camCapturer = (if(isProduce) {
            CameraCapturerFactory.create(
                context,
                fixedResolution = false,
                preferenceFrontCamera = "front" == cameraName
            )
        } else null)!!

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
            it.audioSource = AudioSource.VOICE_COMMUNICATION
            camCapturer?.also { capturer ->
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
        myPeerId = roomData.getString("peerId")
        Log.d(TAG, "myPeerId: $myPeerId")
        serviceCallback.addParticipant(Participant(myPeerId, "You"))
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
            if(!mediaSoupDevice.loaded) {
                mediaSoupDevice.load(routerRtpCapabilities, rtcConfig)
            }
        }
    }

    private suspend fun createTransports() = withContext(Dispatchers.IO) {
        val sctpCapabilities = if(isUseDataChannel) {
            mediaSoupDevice.sctpCapabilities.toJsonObject()
        } else null

        if(isProduce) {
            createSendTransport(sctpCapabilities)
        }

        if(isConsume) {
            createRecvTransport(sctpCapabilities)
        }
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
        //Implementation for enabling microphone
        localAudioManager.initTrack(peerConnectionFactory, mediaConstraintsOption)
        val track = localAudioManager.track ?: run {
            Log.d(TAG, "audio track null")
            return
        }
        withContext(Dispatchers.Main) {
            serviceCallback.updateParticipantAudio(myPeerId, track)
        }
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
        //Implementation for enabling camera
        localVideoManager.initTrack(peerConnectionFactory, mediaConstraintsOption, context)
        camCapturer.startCapture(640, 480, 30)
        val track = localVideoManager.track ?: run {
            Log.d(TAG, "video track null")
            return
        }

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
            Log.d(TAG, "onConnect - recv transport")
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
//            callViewModel.notifyError("Failed to consume producers: ${e.message}")
        }
    }

    private fun consumeNewProducer() {
        try {
            socket.on("new-producer") { args ->
                val newProducer = args[0] as JSONObject
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.addParticipant(
                        Participant(
                            id = newProducer.getString("peerId"),
                            name = "New Peer"
                        )
                    )
                    Log.d(TAG, "New producer: $newProducer")
                    consumeMedia(newProducer.getString("producerId"))
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

            // Emit consumer-resume to the server
            socket.emit("consumer-resume", JSONObject().put("consumerId", id))

            if("video" == consumer.kind) {
                val videoTrack = consumer.track as VideoTrack
                withContext(Dispatchers.Main) {
                    videoTrack.let {
                        serviceCallback.updateParticipantVideo(peerId, it)
                    }
                }
            } else {
                val audioTrack = consumer.track as AudioTrack
                withContext(Dispatchers.Main) {
                    audioTrack.let {
                        serviceCallback.updateParticipantAudio(peerId, it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming media: ${e.message}")
//            callViewModel.notifyError("Failed to consume media: ${e.message}")
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

    fun closeConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendTransport?.close()
                recvTransport?.close()
                socket.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
            } finally {
                sendTransport = null
                recvTransport = null
                socket.close()
                Log.d(TAG, "Socket closed completely")
            }
        }
    }
}