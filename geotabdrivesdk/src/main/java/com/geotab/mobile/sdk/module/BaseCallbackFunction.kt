package com.geotab.mobile.sdk.module

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.DriveSdkConfig
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.UUID

/**
 * Defines the format of data returned from JavaScript calls to the Geotab Drive local JavaScript API
 *
 * @param callerId the id of the caller that invoked the function call
 * @param error exists when an error occurs when running JavaScript
 * @param result exists when a result is returned from the JavaScript
 */
@Keep
data class JavascriptResponse(
    val callerId: String,
    val error: String?,
    val result: String?
)

/**
 * Base type for native functions that call the Geotab Drive local JavaScript API
 *
 * @param name function name
 */
abstract class BaseCallbackFunction(open val name: String) :
    BaseFunction<JavascriptResponse>() {
    val callbacks: HashMap<String, (Result<Success<String>, Failure>) -> Unit> = hashMapOf()

    /**
     * Get the JavaScript to evaluate on the WebView
     *
     * @param context Android [Context], for instantiating a Mustache instance
     * @param callerId String to identify where the JavaScript call should return its value result to
     */
    abstract fun getJavascript(context: Context, callerId: String): String

    /**
     * Verifies that the callerId within the [response] is valid.
     *
     * If the callerId does not exist, the JavaScript caller is notified.
     * If the response contains an error, both the JavaScript caller and native caller are notified.
     *
     * @param response the response from the call to Geotab Drive local JavaScript API
     * @param jsCallback callback to notify JavaScript caller in case of failure
     * @return the original caller from the Native SDK
     */
    fun getSdkCallbackOrInvalidate(
        response: JavascriptResponse,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ): ((Result<Success<String>, Failure>) -> Unit)? {
        val callback = callbacks[response.callerId] ?: run {
            jsCallback(Failure(Error(GeotabDriveError.INVALID_CALL_ERROR)))
            return null
        }
        response.error?.let { it ->
            callback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, it)))
            jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, it)))
            callbacks.remove(response.callerId)
            return null
        }
        return callback
    }

    /**
     * Calls the Geotab Drive local JavaScript API and returns the result to a caller from native Android code.
     * Times out in [DriveSdkConfig.apiCallTimeoutMilli] if no response is returned in time.
     *
     * @param context Android [Context] for instantiating a Mustache instance
     * @param evaluate to evaluate the JavaScript
     * @param callback where the result of the call is returned to
     * @param coroutineScope with the scope being used by the coroutine
     */
    fun callJavascript(
        context: Context,
        evaluate: (String, (String) -> Unit) -> Unit,
        callback: (Result<Success<String>, Failure>) -> Unit,
        coroutineScope: CoroutineScope
    ) {
        val callerId = UUID.randomUUID().toString()
        this.callbacks[callerId] = callback

        coroutineScope.launch {
            val javascript = getJavascript(context, callerId)
            evaluate(javascript) {}

            delay(DriveSdkConfig.apiCallTimeoutMilli)

            callbacks.remove(callerId)?.let { singleCallback ->
                singleCallback(Failure(Error(GeotabDriveError.API_CALL_TIMEOUT_ERROR)))
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<JavascriptResponse>() {}.type
    }
}
