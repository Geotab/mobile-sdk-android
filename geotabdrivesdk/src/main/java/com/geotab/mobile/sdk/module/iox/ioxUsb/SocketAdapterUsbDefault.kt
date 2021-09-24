package com.geotab.mobile.sdk.module.iox.ioxUsb

import android.content.Context
import android.hardware.usb.UsbAccessory
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.iox.AsyncMainExecuterAdapter
import com.geotab.mobile.sdk.module.iox.MainExecuter
import com.geotab.mobile.sdk.module.iox.SocketAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SocketAdapterUsbDefault internal constructor(
    private val adapter: UsbAdapter,
    private val executerAdapter: AsyncMainExecuterAdapter
) : SocketAdapter() {

    constructor(context: Context) : this(UsbAdapterDefault(context), MainExecuter())

    private companion object {
        const val ACC_MANUF = "Geotab"
        const val ACC_MODEL = "IOX USB"
    }

    internal sealed class State(val autoConnect: Boolean) {
        object Idle : State(false)
        class WaitingForConnectivity(autoConnect: Boolean) : State(autoConnect)
        class RequestingPermission(autoConnect: Boolean) : State(autoConnect)
        class Open(
            val accessory: UsbAccessory,
            val inputStream: InputStream,
            val outputStream: OutputStream,
            autoConnect: Boolean
        ) : State(autoConnect)
    }

    internal var state: State = State.Idle
        private set
    private var listener: Listener? = null

    override fun open(listener: Listener, autoConnect: Boolean) {
        if (state !is State.Idle) {
            return
        }
        this.listener = listener

        val accessory = adapter.accessoryList.firstOrNull {
            it.manufacturer == ACC_MANUF && it.model == ACC_MODEL
        }
        when (accessory) {
            null -> failConnection(
                Error(GeotabDriveError.SOCKET_ACCESSORY_NOT_AVAILABLE_EXCEPTION),
                autoConnect
            )
            else -> openAccessory(accessory, autoConnect)
        }
    }

    override fun close() {
        listener = null
        shutdown()
    }

    private fun shutdown() {
        when (val state = state) {
            is State.RequestingPermission -> {
                adapter.cancelPermissionRequest()
            }
            is State.Open -> {
                adapter.close(state.accessory)
                state.inputStream.close()
                state.outputStream.close()
            }
        }
        state = State.Idle
    }

    override fun write(byteArray: ByteArray) {
        adapter.dispatchIO {
            val state = state
            if (state !is State.Open) {
                return@dispatchIO
            }
            val outputStream = state.outputStream
            try {
                outputStream.write(byteArray)
            } catch (e: IOException) {
                adapter.dispatchOnMain {
                    listener?.onWrite(Error(GeotabDriveError.SOCKET_WRITE_EXCEPTION))
                }
                return@dispatchIO
            }
            adapter.dispatchOnMain {
                listener?.onWrite()
            }
        }
    }

    private fun openAccessory(accessory: UsbAccessory, autoConnect: Boolean) {
        if (adapter.hasPermission(accessory)) {
            val streams = adapter.open(accessory) { detachedException ->
                onDetach(detachedException)
            }
            if (streams == null) {
                failConnection(
                    Error(GeotabDriveError.SOCKET_ACCESSORY_NOT_AVAILABLE_EXCEPTION),
                    autoConnect
                )
                return
            }
            val inputStream = streams.first
            state = State.Open(
                accessory,
                inputStream,
                streams.second,
                autoConnect
            )
            listen(inputStream)
            listener?.onOpen()
        } else {
            state = State.RequestingPermission(autoConnect)
            adapter.requestPermission(accessory) { hasPermission ->
                onPermission(accessory, hasPermission)
            }
        }
    }

    private fun listen(inputStream: InputStream) {
        adapter.dispatchIO {
            val state = state
            if (state !is State.Open || state.inputStream != inputStream) {
                return@dispatchIO
            }
            val buffer = ByteArray(512)

            try {
                val byteCount = inputStream.read(buffer)
                if (byteCount > 0) {
                    val bytes = ByteArray(byteCount)
                    System.arraycopy(buffer, 0, bytes, 0, bytes.size)
                    adapter.dispatchOnMain {
                        read(bytes)
                        listen(inputStream)
                    }
                } else {
                    adapter.dispatchOnMain {
                        failConnection(
                            Error(GeotabDriveError.SOCKET_ACCESSORY_DETACHED_EXCEPTION),
                            state.autoConnect
                        )
                    }
                }
            } catch (e: IOException) {
                adapter.dispatchOnMain {
                    failConnection(
                        Error(GeotabDriveError.SOCKET_ACCESSORY_DETACHED_EXCEPTION),
                        state.autoConnect
                    )
                }
            }
        }
    }

    override fun read(byteArray: ByteArray) {
        listener?.onRead(byteArray)
    }

    private fun onPermission(accessory: UsbAccessory, hasPermission: Boolean) {
        when (state) {
            is State.RequestingPermission -> {
                if (hasPermission) {
                    openAccessory(accessory, state.autoConnect)
                } else {
                    failConnection(
                        Error(GeotabDriveError.SOCKET_PERMISSION_DENIED_EXCEPTION),
                        state.autoConnect
                    )
                }
            }
        }
    }

    private fun onDetach(exception: Error) {
        when (state) {
            is State.Open, is State.RequestingPermission -> {
                failConnection(exception, state.autoConnect)
            }
        }
    }

    private fun failConnection(exception: Error, autoConnect: Boolean) {
        if (state is State.WaitingForConnectivity) {
            return
        }
        val previousState = state
        val listener = listener
        shutdown()
        if (autoConnect) {
            listener?.let {
                state = State.WaitingForConnectivity(autoConnect)
                waitForConnectivity(it)
            }
        } else {
            when (previousState) {
                is State.Idle -> listener?.onOpen(exception)
                is State.RequestingPermission -> listener?.onOpen(exception)
                else -> listener?.onCloseUnexpectedly(exception)
            }
        }
    }

    private fun waitForConnectivity(listener: Listener) {
        executerAdapter.after(5) {
            if (state !is State.WaitingForConnectivity) {
                return@after
            }
            val autoConnect = state.autoConnect
            state = State.Idle
            open(listener, autoConnect)
        }
    }
}
