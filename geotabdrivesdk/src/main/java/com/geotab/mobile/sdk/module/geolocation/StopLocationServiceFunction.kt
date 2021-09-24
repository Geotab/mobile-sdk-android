package com.geotab.mobile.sdk.module.geolocation
import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class StopLocationServiceFunction(val context: Context, override val name: String = "___stopLocationService", override val module: GeolocationModule) :
    ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        try {
            module.stopService()
            jsCallback(Success("undefined"))
        } catch (e: Error) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, GeolocationModule.STOP_SERVICE_FAIL)))
        }
    }
}
