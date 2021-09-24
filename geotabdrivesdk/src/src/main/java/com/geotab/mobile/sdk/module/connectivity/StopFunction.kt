package com.geotab.mobile.sdk.module.connectivity

import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class StopFunction(override val name: String = "stop", override val module: ConnectivityModule) : ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val result = stop()
        jsCallback(Success("$result"))
    }

    private fun stop(): Boolean {
        val result = module.unRegisterConnectivityActionReceiver()
        if (result) module.started = false
        return result
    }
}
