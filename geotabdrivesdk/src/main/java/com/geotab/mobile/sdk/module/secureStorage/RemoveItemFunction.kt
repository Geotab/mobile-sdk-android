package com.geotab.mobile.sdk.module.secureStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.secureStorage.SecureStorageModule.Companion.ERROR_REMOVING_KEY
import com.geotab.mobile.sdk.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class RemoveItemFunction(
    override val name: String = "removeItem",
    val secureStorageRepository: SecureStorageRepository,
    override val module: SecureStorageModule
) : ModuleFunction, BaseFunction<String>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val arguments = jsonString?.takeIf { it.isNotBlank() } ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            val result = secureStorageRepository.delete(arguments)
            if (result == 0) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, ERROR_REMOVING_KEY)))
                return@launch
            }
            jsCallback(Success(JsonUtil.toJson(arguments)))
        }
    }

    override fun getType(): Type {
        return object : TypeToken<String>() {}.type
    }
}
