package com.geotab.mobile.sdk.module.connectivity

import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class StartFunction(override val name: String = "start", override val module: ConnectivityModule) : ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val result = start()
        jsCallback(Success("$result"))
    }

    private fun start(): Boolean {
        if (module.started) {
            return module.started
        }
        val result = module.registerConnectivityActionReceiver()
        module.started = result
        return result
    }
}
