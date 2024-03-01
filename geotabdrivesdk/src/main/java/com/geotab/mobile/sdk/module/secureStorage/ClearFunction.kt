package com.geotab.mobile.sdk.module.secureStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.launch

class ClearFunction(
    override val name: String = "clear",
    val secureStorageRepository: SecureStorageRepository,
    override val module: SecureStorageModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val result = secureStorageRepository.deleteAll()
            if (result == 0) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, SecureStorageModule.ERROR_REMOVING_ALL)))
                return@launch
            }
            jsCallback(Success("\"success\""))
        }
    }
}
