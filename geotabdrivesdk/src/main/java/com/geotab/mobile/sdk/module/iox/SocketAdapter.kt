package com.geotab.mobile.sdk.module.iox

import com.geotab.mobile.sdk.Error

abstract class SocketAdapter {
    var uuidStr: String? = null

    interface Listener {
        fun onOpen(exception: Error? = null)
        fun onCloseUnexpectedly(exception: Error)
        fun onRead(byteArray: ByteArray?, exception: Error? = null)
        fun onWrite(exception: Error? = null)
        fun onDisconnect()
    }

    abstract fun open(listener: Listener, autoConnect: Boolean = false)
    abstract fun close()
    abstract fun write(byteArray: ByteArray)
    abstract fun read(byteArray: ByteArray)
}
