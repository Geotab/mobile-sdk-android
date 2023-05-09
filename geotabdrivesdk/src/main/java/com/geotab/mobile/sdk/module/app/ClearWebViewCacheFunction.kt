package com.geotab.mobile.sdk.module.app

import android.webkit.WebStorage
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class ClearWebViewCacheFunction(
    override val name: String = "clearWebViewCache",
    override val module: AppModule
) : ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        // In addition to deleting the cache, deleting web storage is necessary to remove the session.
        // This link suggests that this should be enough to clear the service workers as well, but it does not seem to.
        // https://stackoverflow.com/questions/39202258/clear-service-worker-cache-from-android-api/39211131#39211131
        WebStorage.getInstance().deleteAllData()
        if (module.clearCacheDirs()) {
            jsCallback(Success("undefined"))
        } else {
            jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION)))
        }
    }
}
