package com.geotab.mobile.sdk.module.browser

import android.content.Context
import android.content.Intent
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class CloseBrowserWindowFunction(override val name: String = BrowserModule.closeFunctionName, override val module: BrowserModule, private val context: Context) : ModuleFunction {

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        module.launch {
            val closeIntent = Intent("close")
            context.applicationContext.sendBroadcast(closeIntent)
        }
    }
}
