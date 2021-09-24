package com.geotab.mobile.sdk.util

import java.nio.ByteBuffer

internal fun ByteArray.toByteBuffer(index: Int, size: Int): ByteBuffer {
    val bytesToWrap = ByteArray(size)
    System.arraycopy(this, index, bytesToWrap, 0, size)
    return ByteBuffer.wrap(bytesToWrap).order(java.nio.ByteOrder.LITTLE_ENDIAN)
}

internal fun IntArray.toByteArray(): ByteArray = this.map {
    it.toByte()
}.toByteArray()
