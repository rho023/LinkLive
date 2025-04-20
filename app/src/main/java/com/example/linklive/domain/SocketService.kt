package com.example.linklive.domain

interface SocketService {
    fun initializeSocket(url: String)
    fun connect()
    fun disconnect()
    fun getSocket(): io.socket.client.Socket
}