package com.geotab.mobile.sdk.module.iox.ioxBle

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class StartIoxBleFunction(
    override val module: IoxBleModule,
    override val name: String = "start"
) : ModuleFunction {
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
            val uuid = jsonString?.takeIf { it.isNotBlank() } ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            module.start(uuid, jsCallback)
        }
    }
}
