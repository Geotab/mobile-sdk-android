package com.geotab.mobile.sdk.module.secureStorage

import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class LengthFunction(
    override val name: String = "length",
    val secureStorageRepository: SecureStorageRepository,
    override val module: SecureStorageModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val result = secureStorageRepository.length()
            jsCallback(Success(result.toString()))
        }
    }
}
