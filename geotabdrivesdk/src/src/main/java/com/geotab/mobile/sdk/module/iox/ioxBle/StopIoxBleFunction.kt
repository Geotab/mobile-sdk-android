package com.geotab.mobile.sdk.module.iox.ioxBle

import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class StopIoxBleFunction(
    override val module: IoxBleModule,
    override val name: String = "stop"
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
            module.stop(jsCallback)
        }
    }
}
