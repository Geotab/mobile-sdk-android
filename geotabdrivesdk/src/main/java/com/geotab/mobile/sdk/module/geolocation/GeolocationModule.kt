package com.geotab.mobile.sdk.module.geolocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.location.LocationManagerCompat
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.GeolocationCoordinates
import com.geotab.mobile.sdk.models.GeolocationPosition
import com.geotab.mobile.sdk.models.GeolocationResult
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.util.JsonUtil
import java.io.ByteArrayOutputStream
import java.util.Date

class GeolocationModule(
    val context: Context,
    permissionDelegate: PermissionDelegate,
    private val evaluate: (String, (String) -> Unit) -> Unit,
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit
) : Module(MODULE_NAME), android.location.LocationListener {

    companion object {
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val POSITION_UNAVAILABLE = "POSITION_UNAVAILABLE"
        const val STOP_SERVICE_FAIL = "Failed to stop location service"
        const val templateFileName = "GeolocationModule.Script.js"
        const val INTERVAL = 0.toLong()
        const val DISTANCE = 0.toFloat()
        const val TAG = "GeolocationModule"
        const val MODULE_NAME = "geolocation"
    }

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val criteria: Criteria = Criteria()
    val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
    }

    var started = false
    private var isHighAccuracyEnabled = false
    private var lastLocationUpdate = GeolocationResult(null, null)

    init {
        functions.add(StartLocationServiceFunction(context, module = this))
        functions.add(StopLocationServiceFunction(context, module = this))
    }

    fun isLocationServicesEnabled(): Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun updateLocation(geolocationResult: GeolocationResult) {
        val outputStream = ByteArrayOutputStream()
        JsonUtil.toJsonStreaming(outputStream, geolocationResult)
        val json = String(outputStream.toByteArray())

        val script = """
            if (window.$geotabModules != null && window.$geotabModules.$name != null) {
                window.$geotabModules.$name.result = $json;
            }
            """.trimMargin()
        evaluate(script) {}
        push(ModuleEvent("geolocation.result", "{ detail: $json }")) {}
    }

    @SuppressLint("MissingPermission")
    fun startService(isAccuracyFine: Boolean) {

        if (started && isHighAccuracyEnabled) {
            return
        }

        if (isAccuracyFine) {
            isHighAccuracyEnabled = true
        }
        if (isHighAccuracyEnabled) {
            criteria.accuracy = Criteria.ACCURACY_FINE
        } else {
            criteria.accuracy = Criteria.ACCURACY_COARSE
        }

        try {
            if (started) {
                this.stopService()
            }
            locationManager.requestLocationUpdates(INTERVAL, DISTANCE, criteria, this, Looper.getMainLooper())
            started = true
        } catch (e: SecurityException) {
            throw Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, POSITION_UNAVAILABLE)
        }
    }

    fun stopService() {
        isHighAccuracyEnabled = false
        try {
            if (started) {
                locationManager.removeUpdates(this)
                started = false
            } else {
                return
            }
        } catch (e: Exception) {
            throw Exception(Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, STOP_SERVICE_FAIL))
        }
    }

    override fun scripts(context: Context): String {

        var scripts = super.scripts(context)
        val scriptData: HashMap<String, Any> = hashMapOf(
            "geotabModules" to geotabModules,
            "moduleName" to name,
            "geotabNativeCallbacks" to geotabNativeCallbacks,
            "callbackPrefix" to callbackPrefix
        )
        scripts += getScriptFromTemplate(context, templateFileName, scriptData)
        val json = JsonUtil.toJson(lastLocationUpdate)
        scripts += """window.$geotabModules.$name.result = $json;"""
        return scripts
    }

    override fun onLocationChanged(location: Location) {
        val coordinates = GeolocationCoordinates(
            location.latitude,
            location.longitude,
            location.altitude,
            location.accuracy.toDouble(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.verticalAccuracyMeters.toDouble() else null,
            location.bearing.toDouble(),
            location.speed.toDouble()
        )

        try {
            val validatedCoordinates = GeolocationCoordinates(
                coordinates.geolocationLatitude,
                coordinates.geolocationLongitude,
                coordinates.geolocationAltitude,
                coordinates.geolocationAccuracy,
                coordinates.geolocationAltitudeAccuracy,
                coordinates.geolocationHeading,
                coordinates.geolocationSpeed
            )

            val position = GeolocationPosition(validatedCoordinates, Date().time)
            val result = GeolocationResult(position, null)
            updateLocation(result)
        } catch (e: Exception) {
            updateLocation(GeolocationResult(null, POSITION_UNAVAILABLE))
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError onLocationChange", e)
            updateLocation(GeolocationResult(null, POSITION_UNAVAILABLE))
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {
        updateLocation(GeolocationResult(null, POSITION_UNAVAILABLE))
    }
}
