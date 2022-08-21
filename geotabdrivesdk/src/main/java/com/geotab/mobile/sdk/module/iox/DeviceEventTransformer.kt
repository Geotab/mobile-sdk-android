package com.geotab.mobile.sdk.module.iox

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.DeviceEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.util.toByteBuffer
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.experimental.and

class DeviceEventTransformer {

    companion object {
        private val DRIVE_DATE_FORMAT =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
        const val JAN_1_2002_TIMESTAMP = 1009843200
        const val MILLISECONDS = 1_000
        const val LOCATION_PRECISION = 10_000_000
        const val RPM_PRECISION = 4
        const val ODOMETER_PRECISION = 10
        const val ENGINE_HOURS_PRECISION = 10
    }

    fun transform(byteArray: ByteArray): DeviceEvent {
        if (byteArray.size < 40) {
            throw Error(GeotabDriveError.EVENT_PARSING_EXCEPTION)
        }
        val dateTime =
            DRIVE_DATE_FORMAT.format(
                (byteArray.toByteBuffer(0, 4).int.toLong() + JAN_1_2002_TIMESTAMP) * MILLISECONDS
            )
        val latitude = byteArray.toByteBuffer(4, 4).int.toFloat() / LOCATION_PRECISION

        val longitude = byteArray.toByteBuffer(8, 4).int.toFloat() / LOCATION_PRECISION

        val roadSpeed = byteArray[12].toFloat()

        val rpm = byteArray.toByteBuffer(13, 2).short.toFloat() / RPM_PRECISION

        val odometer = byteArray.toByteBuffer(15, 4).int.toFloat() / ODOMETER_PRECISION

        val status = statusFlags(byteArray[19])

        val tripOdometer = byteArray.toByteBuffer(20, 4).int.toFloat() / ODOMETER_PRECISION

        val engineHours = byteArray.toByteBuffer(24, 4).int.toFloat() / ENGINE_HOURS_PRECISION

        val tripDuration = byteArray.toByteBuffer(28, 4).int.toLong() * MILLISECONDS

        val vehicleId = byteArray.toByteBuffer(32, 4).int.toString()

        val driverId = byteArray.toByteBuffer(36, 4).int.toString()

        return DeviceEvent(
            dateTime,
            latitude,
            longitude,
            roadSpeed,
            rpm,
            status,
            odometer,
            tripOdometer,
            engineHours,
            tripDuration,
            vehicleId,
            driverId,
            byteArray
        )
    }

    private fun statusFlags(byte: Byte): String {

        val sb = StringBuilder()

        sb.append(
            if (byte and 0b0000_0001 != 0x00.toByte()) {
                "GPS Latched"
            } else {
                "GPS Invalid"
            }
        ).append(" | ")

        sb.append(
            if (byte and 0b0000_0010 != 0.toByte()) {
                "IGN on"
            } else {
                "IGN off"
            }
        ).append(" | ")

        sb.append(
            if (byte and 0b0000_0100 != 0.toByte()) {
                "Engine Data"
            } else {
                "No Engine Data"
            }
        ).append(" | ")

        sb.append(
            if (byte and 0b0000_1000 != 0.toByte()) {
                "Date/Time Valid"
            } else {
                "Date/Time Invalid"
            }
        ).append(" | ")

        sb.append(
            if (byte and 0b0001_0000 != 0.toByte()) {
                "Speed From Engine"
            } else {
                "Speed From GPS"
            }
        ).append(" | ")

        sb.append(
            if (byte and 0b0010_0000 != 0.toByte()) {
                "Distance From Engine"
            } else {
                "Distance From GPS"
            }
        )
        return sb.toString()
    }
}
