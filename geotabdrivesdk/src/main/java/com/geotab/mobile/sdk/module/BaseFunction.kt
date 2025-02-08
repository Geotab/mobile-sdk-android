package com.geotab.mobile.sdk.module

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type

/**
 * Base class for validating JavaScript function input
 *
 * @param T the type of object the JSON String will be transformed into
 */
abstract class BaseFunction<T> {
    abstract fun getType(): Type

    /**
     * Transforms a JSON string into an object of type [T]
     * If the JSON string is of unexpected format, calls [jsCallback] callback with [Failure]
     *
     * @param json JSON String passed by the WebView to be transformed into [T]
     * @param jsCallback callback to notify JavaScript caller of failure
     */
    fun transformOrInvalidate(json: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit): T? {
        json ?: run {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
            return null
        }
        return try {
            // Handling null result because of an empty String
            // https://github.com/google/gson/issues/457
            transform(json) ?: throw JsonSyntaxException("Null Transformation")
        } catch (e: JsonSyntaxException) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, e.message)))
            null
        }
    }

    @Throws(JsonSyntaxException::class)
    // TODO: make private, refactor test
    fun transform(json: String): T {
        return Gson().fromJson(json, getType())
    }
}
