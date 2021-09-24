package com.geotab.mobile.sdk.models

data class GeolocationResult(
    val position: GeolocationPosition?,
    val error: String?
)

data class GeolocationPosition(
    val coords: GeolocationCoordinates?,
    val timestamp: Long
)

data class GeolocationCoordinates(
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracy: Double?,
    val altitudeAccuracy: Double?,
    val heading: Double?,
    val speed: Double?

)
