package com.geotab.mobile.sdk.util

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.Gson

class PushScriptUtil {
    fun validEvent(moduleEvent: ModuleEvent, callBack: ((Result<Success<String>, Failure>) -> Unit)): Boolean {
        if (moduleEvent.event.contains("\"") || moduleEvent.event.contains("\'")) {
            callBack(Failure(Error(GeotabDriveError.INVALID_MODULE_EVENT)))
            return false
        }

        try {
            Gson().fromJson(moduleEvent.params, Object::class.java)
        } catch (exception: Exception) {
            callBack(
                Failure(
                    Error(GeotabDriveError.INVALID_JSON)
                )
            )

            return false
        }

        return true
    }
}
