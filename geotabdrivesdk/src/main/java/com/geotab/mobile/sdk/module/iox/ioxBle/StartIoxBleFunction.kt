package com.geotab.mobile.sdk.module.iox.ioxBle

import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class StartIoxBleOptions(
    val uuid: String?,
    val reconnect: Boolean = false,
)

class StartIoxBleFunction(
    override val module: IoxBleModule,
    override val name: String = "start"
) : ModuleFunction, BaseFunction<StartIoxBleOptions>() {

    /**
     * Handle incoming JavaScript calls from Geotab Drive's web component
     *
     * @param jsonString object from JavaScript caller to parse
     * @param jsCallback callback to notify JavaScript caller of [Success] or [Failure]
     */
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch {
            val startIoxBleOptions = transformOrInvalidate(jsonString, jsCallback)
                ?: return@launch

            // Check if the Gson Transformer converts to null value.
            if (startIoxBleOptions.uuid.isNullOrBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            module.start(startIoxBleOptions, jsCallback)
        }
    }

    override fun getType(): Type {
        return object : TypeToken<StartIoxBleOptions>() {}.type
    }
}
