package com.geotab.mobile.sdk.module.geolocation
import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.util.JsonUtil
import java.lang.Exception

@Keep
data class StartFunctionArgument(
    val enableHighAccuracy: Boolean? = null
)

class StartLocationServiceFunction(val context: Context, override val name: String = "___startLocationService", override val module: GeolocationModule) :
    ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit

    ) {
        val argument = jsonString?.let {
            try {
                JsonUtil.fromJson<StartFunctionArgument>(it)
            } catch (e: Exception) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return
            }
        }

        if (!module.isLocationServicesEnabled()) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, GeolocationModule.POSITION_UNAVAILABLE)))
            return
        }

        if (module.permissionHelper.hasPermission(arrayOf(Permission.LOCATION))) {
            try {
                module.startService(argument?.enableHighAccuracy ?: false)
                jsCallback(Success("undefined"))
            } catch (e: Error) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, GeolocationModule.POSITION_UNAVAILABLE)))
            }
        } else {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_GEOLOCATION_ERROR, GeolocationModule.PERMISSION_DENIED)))
        }
    }
}
