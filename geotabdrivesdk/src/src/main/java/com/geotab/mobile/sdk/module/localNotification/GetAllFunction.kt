package com.geotab.mobile.sdk.module.localNotification

import com.geotab.mobile.sdk.models.NativeNotify
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.repository.PreferenceNotificationRepository
import com.geotab.mobile.sdk.util.JsonUtil

class GetAllFunction(override val name: String = "getAll", override val module: LocalNotificationModule, val prefsNotificationRepository: PreferenceNotificationRepository) :
    ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        getAll { result ->
            jsCallback(Success(JsonUtil.toJson(result)))
        }
    }

    private fun getAll(result: (List<NativeNotify>) -> Unit) {
        return result(prefsNotificationRepository.getAll())
    }
}
