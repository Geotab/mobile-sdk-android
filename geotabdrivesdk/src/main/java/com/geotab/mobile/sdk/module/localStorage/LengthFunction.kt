package com.geotab.mobile.sdk.module.localStorage

import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageRepository
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class LengthFunction(
    override val name: String = "length",
    val localStorageRepository: LocalStorageRepository,
    override val module: LocalStorageModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val result = localStorageRepository.length()
            jsCallback(Success(result.toString()))
        }
    }
}
