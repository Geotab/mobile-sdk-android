package com.geotab.mobile.sdk.module.secureStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import com.geotab.mobile.sdk.util.decryptText
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class GetItemFunction(
    override val name: String = "getItem",
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

            try {
                val byteResult = secureStorageRepository.getValue(arguments)
                if (byteResult == null) {
                    jsCallback(
                        Failure(
                            Error(
                                GeotabDriveError.STORAGE_MODULE_ERROR,
                                SecureStorageModule.ERROR_GETTING_VALUE
                            )
                        )
                    )
                    return@launch
                }
                val result = decryptText(module.keyAlias, byteResult)
                jsCallback(Success(JsonUtil.toJson(result)))
            } catch (e: Exception) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, e.message)))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<String>() {}.type
    }
}
