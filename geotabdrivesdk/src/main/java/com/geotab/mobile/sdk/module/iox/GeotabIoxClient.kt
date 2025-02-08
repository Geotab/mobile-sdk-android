package com.geotab.mobile.sdk.module.iox

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.DeviceEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.iox.ioxBle.SocketAdapterBleDefault

class GeotabIoxClient(
    private val socket: SocketAdapter,
    private val transformer: DeviceEventTransformer,
    private val executerAdapter: AsyncMainExecuterAdapter
) : SocketAdapter.Listener {

    private var listener: Listener? = null
    var state: State = State.Idle
        set(value) {
            field = value
            listener?.onStateUpdate(state)
        }

    var uuidStr: String? = null
        set(value) {
            socket.uuidStr = value
            field = value
        }

    var reconnect: Boolean = false

    sealed class State {
        object Idle : State()
        object Opening : State()
        object Syncing : State()
        class Handshaking(val previouslyConnected: Boolean) : State()
        object Connected : State()
    }

    interface Listener {
        fun onStart(exception: Error? = null)
        fun onStoppedUnexpectedly(exception: Error)
        fun onEvent(deviceEvent: DeviceEvent?, exception: Error? = null)
        fun onDisconnect()
        fun onStateUpdate(state: State)
    }

    fun start(listener: Listener) {
        if (state != State.Idle) {
            listener.onStart(
                Error(
                    GeotabDriveError.MODULE_BLE_ERROR,
                    SocketAdapterBleDefault.BLE_ALREADY_CONNECTED
                )
            )
            return
        }
        this.listener = listener
        state = State.Opening
        socket.open(this, true)
    }

    fun stop() {
        state = State.Idle
        listener = null
        socket.close()
    }

    override fun onOpen(exception: Error?) {
        exception?.let {
            state = State.Idle
            this.listener?.also {
                listener = null
                it.onStoppedUnexpectedly(exception)
            }
            return
        }
        state = State.Syncing
        sendSyncMessage()
    }

    override fun onCloseUnexpectedly(exception: Error) {
        listener?.also {
            if (exception.message != SocketAdapterBleDefault.BLE_DISCONNECTED) {
                this.listener = null
            }

            it.onStoppedUnexpectedly(exception)

            if (reconnect) {
                state = State.Opening
                socket.open(this, true)
            } else {
                state = State.Idle
            }
        }
    }

    override fun onRead(byteArray: ByteArray?, exception: Error?) {
        exception?.let {
            when (state) {
                is State.Connected -> {
                    listener?.onEvent(null, exception)
                }
                is State.Opening, is State.Syncing, is State.Handshaking -> {
                    listener?.also {
                        state = State.Idle
                        this.listener = null
                        it.onStoppedUnexpectedly(exception)
                    }
                }
                State.Idle -> {}
            }
            return
        }

        if (byteArray == null) {
            listener?.onEvent(null, Error(GeotabDriveError.EVENT_PARSING_EXCEPTION))
            return
        }

        when (val state = state) {
            is State.Syncing -> {
                if (byteArray.contentEquals(HANDSHAKE.data)) {
                    this.state = State.Handshaking(false)
                    socket.write(HandshakeConfirmationMessage().data)
                }
            }
            is State.Handshaking -> {
                if (byteArray.contentEquals(ACK.data)) {
                    this.state = State.Connected
                    if (!state.previouslyConnected) {
                        listener?.onStart()
                    }
                }
            }
            is State.Connected -> {
                if (byteArray.contentEquals(HANDSHAKE.data)) {
                    this.state = State.Handshaking(true)
                    socket.write(HandshakeConfirmationMessage().data)
                    return
                }
                val message = try {
                    GoDeviceDataMessage(byteArray)
                } catch (e: IllegalArgumentException) {
                    listener?.onEvent(null, Error(GeotabDriveError.EVENT_PARSING_EXCEPTION))
                    return
                }
                val event = transformer.transform(message.payload)
                listener?.onEvent(event)
            }
            State.Idle -> {}
            State.Opening -> {}
        }
    }

    override fun onWrite(exception: Error?) {
        when (state) {
            is State.Opening, is State.Syncing, is State.Handshaking -> {
                if (exception != null) {
                    listener?.also {
                        state = State.Idle
                        this.listener = null
                        it.onStoppedUnexpectedly(exception)
                    }
                }
            }
            State.Connected -> {}
            State.Idle -> {}
        }
    }

    override fun onDisconnect() {
        listener?.onDisconnect()
    }

    private fun sendSyncMessage() {
        when (state) {
            is State.Syncing -> {
                socket.write(SyncMessage().data)
                executerAdapter.after(1) {
                    sendSyncMessage()
                }
            }
            State.Connected -> {}
            is State.Handshaking -> {}
            State.Idle -> {}
            State.Opening -> {}
        }
    }
}
