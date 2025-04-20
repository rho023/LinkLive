package com.example.linklive.data

import com.example.linklive.domain.SocketService
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketHandler @Inject constructor() : SocketService {
    private lateinit var socket: Socket

    override fun initializeSocket(url: String) {
        socket = IO.socket(url)
    }

    override fun getSocket(): Socket {
        return socket
    }

    override fun connect() {
        if(::socket.isInitialized) {
            socket.connect()
        } else {
            throw IllegalStateException("Socket not initialized. Call initializeSocket() first.")
        }
    }

    override fun disconnect() {
        if(::socket.isInitialized) {
            socket.disconnect()
        } else {
            throw IllegalStateException("Socket not initialized. Call initializeSocket() first.")
        }
    }
}