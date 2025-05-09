package com.example.linklive.utils

import org.webrtc.EglBase

object PeerConnectionUtils {
    private val eglBase = EglBase.create()
    internal val eglContext: EglBase.Context = eglBase.eglBaseContext
}