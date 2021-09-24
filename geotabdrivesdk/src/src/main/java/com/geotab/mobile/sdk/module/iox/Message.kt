package com.geotab.mobile.sdk.module.iox

import com.geotab.mobile.sdk.util.toByteArray

const val DEVICE_MESSAGE_TYPE: Byte = 0x21.toByte()
const val HANDSHAKE_CONFIRMATION_MESSAGE_TYPE: Byte = 0x81.toByte()

private interface Message {
    val data: ByteArray
}

internal fun checksum(bytes: ByteArray): ByteArray {
    var check1: Byte = 0x00
    var check2: Byte = 0x00
    for (byte in bytes) {
        check1 = check1.toInt().plus(byte.toInt()).and(0xFF).toByte()
        check2 = check2.toInt().plus(check1.toInt()).and(0xFF).toByte()
    }
    return byteArrayOf(check1, check2)
}

internal class SyncMessage : Message {
    override val data: ByteArray = byteArrayOf(0x55.toByte())
}

internal abstract class ChecksumMessage(val messageType: Byte, val payload: ByteArray) : Message {

    companion object {
        const val STX: Byte = 0x02.toByte()
        const val ETX: Byte = 0x03.toByte()
    }

    override val data: ByteArray
        get() {
            var bytes = byteArrayOf(STX, messageType)
            bytes += (payload.size and 0xFF).toByte()
            bytes += payload
            bytes += checksum(bytes)
            bytes += ETX
            return bytes
        }
}

internal class HandshakeConfirmationMessage : ChecksumMessage(HANDSHAKE_CONFIRMATION_MESSAGE_TYPE, DEVICE_ID + FLAGS) {
    companion object {
        val DEVICE_ID: ByteArray = intArrayOf(0x2D, 0x10).toByteArray()
        val FLAGS: ByteArray = intArrayOf(0x01, 0x00).toByteArray()
    }
}

internal object HANDSHAKE {
    val data: ByteArray = intArrayOf(
        0x02,
        0x01,
        0x00,
        0x03,
        0x08,
        0x03
    ).toByteArray()
}

internal object ACK {
    val data: ByteArray = intArrayOf(
        0x02,
        0x02,
        0x00,
        0x04,
        0x0A,
        0x03
    ).toByteArray()
}

internal fun isGoDeviceData(data: ByteArray): Boolean = data.size >= 6 && data[1] == DEVICE_MESSAGE_TYPE

internal class GoDeviceDataMessage(data: ByteArray) : ChecksumMessage(DEVICE_MESSAGE_TYPE, data.sliceArray(3 until data.size - 3)) {
    init {
        require(data.size >= 6)
        require(data[0] == STX)
        require(data[1] == messageType)
        require(data.last() == ETX)
        val length = data[2].toInt()
        require(length + 6 == data.size)
        val calculatedChecksum = checksum(data.sliceArray(0 until data.size - 3))
        require(data.sliceArray(data.size - 3 until data.size - 1).contentEquals(calculatedChecksum))
    }
}
