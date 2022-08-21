package com.geotab.mobile.sdk.models

import com.geotab.mobile.sdk.module.geolocation.GeolocationModule

data class GeolocationResult(
    val position: GeolocationPosition?,
    val error: String?
)

data class GeolocationPosition(
    val coords: GeolocationCoordinates?,
    val timestamp: Long
)

data class GeolocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Double?,
    val altitudeAccuracy: Double?,
    val heading: Double?,
    val speed: Double?

) {
    val geolocationLatitude
        get() = if (latitude.isNaN() || latitude == null) throw Exception(GeolocationModule.POSITION_UNAVAILABLE) else latitude
    val geolocationLongitude
        get() = if (longitude.isNaN() || longitude == null) throw Exception(GeolocationModule.POSITION_UNAVAILABLE) else longitude
    val geolocationAltitude
        get() = if (altitude?.isNaN() == true) null else altitude
    val geolocationAccuracy
        get() = if (accuracy?.isNaN() == true) null else accuracy
    val geolocationAltitudeAccuracy
        get() = if (altitudeAccuracy?.isNaN() == true) null else altitudeAccuracy
    val geolocationHeading
        get() = if (heading?.isNaN() == true) null else heading
    val geolocationSpeed
        get() = if (speed?.isNaN() == true) null else speed
}
