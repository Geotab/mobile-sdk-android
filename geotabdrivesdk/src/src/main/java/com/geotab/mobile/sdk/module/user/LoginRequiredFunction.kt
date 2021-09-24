package com.geotab.mobile.sdk.module.user

import com.geotab.mobile.sdk.models.LoginRequiredArgument
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class LoginRequiredFunction(override val name: String = "loginRequired", override val module: UserModule) : ModuleFunction,
    BaseFunction<LoginRequiredArgument>() {

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        module.loginRequiredCallback(response)
        jsCallback(Success("undefined"))
    }

    override fun getType(): Type {
        return object : TypeToken<LoginRequiredArgument>() {}.type
    }
}
