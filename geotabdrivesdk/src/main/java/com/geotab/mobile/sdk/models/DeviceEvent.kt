package com.geotab.mobile.sdk.models

/**
 * @property dateTime Time of event in milliseconds since the Unix epoch
 * @property latitude Latitude in degrees
 * @property longitude Longitude in degrees
 * @property roadSpeed Speed of vehicle in km/h
 * @property rpm Engine revolutions per minute
 * @property status Debug status
 * @property odometer Vehicle odometer reading in kilometers
 * @property tripOdometer Vehicle trip odometer reading in kilometers
 * @property engineHours Engine hours
 * @property tripDuration Vehicle trip duration in milliseconds
 * @property vehicleId Identifier for vehicle
 * @property driverId Identifier for driver
 */
data class DeviceEvent(
    val dateTime: String,
    val latitude: Float,
    val longitude: Float,
    val roadSpeed: Float,
    val rpm: Float,
    val status: String,
    val odometer: Float,
    val tripOdometer: Float,
    val engineHours: Float,
    val tripDuration: Long,
    val vehicleId: String,
    val driverId: String,
    val rawData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceEvent

        if (dateTime != other.dateTime) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (roadSpeed != other.roadSpeed) return false
        if (rpm != other.rpm) return false
        if (status != other.status) return false
        if (odometer != other.odometer) return false
        if (tripOdometer != other.tripOdometer) return false
        if (engineHours != other.engineHours) return false
        if (tripDuration != other.tripDuration) return false
        if (vehicleId != other.vehicleId) return false
        if (driverId != other.driverId) return false
        if (!rawData.contentEquals(other.rawData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dateTime.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + roadSpeed.hashCode()
        result = 31 * result + rpm.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + odometer.hashCode()
        result = 31 * result + tripOdometer.hashCode()
        result = 31 * result + engineHours.hashCode()
        result = 31 * result + tripDuration.hashCode()
        result = 31 * result + vehicleId.hashCode()
        result = 31 * result + driverId.hashCode()
        result = 31 * result + rawData.contentHashCode()
        return result
    }
}
