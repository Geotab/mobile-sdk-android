package com.geotab.mobile.sdk

import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.auth.AuthUtil

/**
 * Base class for Geotab fragments that interact with WebView via JavaScript bridge.
 * Provides lifecycle-safe execution to prevent JNI reference leaks (MOB-3805).
 */
abstract class BaseGeotabFragment : Fragment() {
    companion object {
        const val TAG: String = "BaseGeotabFragment"
    }
    /**
     * Returns the WebView instance managed by this fragment.
     */
    protected abstract fun getWebView(): WebView?

    /**
     * Returns the Lifecycle to use for validation checks.
     * Can be overridden in tests to inject a mock lifecycle.
     */
    protected open fun getLifecycleForValidation(): Lifecycle = lifecycle

    /**
     * Executes the block only if Fragment lifecycle is at least CREATED and WebView is attached.
     * Prevents JNI global reference leaks by skipping execution when Fragment is destroyed.
     *
     * **Important:** This function performs a non-local return when conditions are invalid,
     * meaning the calling function will exit immediately without executing subsequent code.
     *
     * Example:
     * ```kotlin
     * fun myFunction() {
     *  executeIfValid {
     *      // This code only runs if Fragment is valid
     *  }
     *  // ⚠️ This code will NOT run if executeIfValid's conditions fail!
     * }
     *  ```
     * @param callback Code to execute if conditions are valid
     */
    protected inline fun executeIfValid(callback: () -> Unit) {
        if (!getLifecycleForValidation().currentState.isAtLeast(Lifecycle.State.CREATED) ||
            getWebView()?.isAttachedToWindow != true
        ) {
            Logger.shared.error(TAG, "Fragment is destroyed or WebView is detached; skipping execution to prevent JNI reference leak.")
            return // Non-local return - exits the calling function
        }
        callback()
    }

    /**
     * Builds JavaScript code to invoke error callback with proper error formatting.
     * Supports both simple errors and structured JSON errors (e.g., AuthError).
     *
     * @param callback JavaScript callback function name
     * @param reason Error object containing error code and message
     * @return JavaScript code string to execute
     */
    protected fun buildErrorJavaScript(callback: String, reason: Error): String {
        val errorMessage = reason.getErrorMessage()

        // Check if error message is structured JSON (starts with '{' and contains required fields)
        val isStructuredError = AuthUtil.isStructuredAuthError(errorMessage)

        return if (!isStructuredError) {
            // Basic error message - create simple Error object
            val fullMessage = "${reason.getErrorCode()}: $errorMessage"
            """ try {
                var t = $callback(new Error(`$fullMessage`));
                if (t instanceof Promise) {
                    t.catch(err => { console.log(">>>>> Unexpected exception in Promise: ", err); });
                }
            } catch(err) {
                console.log(">>>>> Unexpected exception in callback: ", err);
            }
            """.trimIndent()
        } else {
            // Structured error with metadata - directly use JSON as JavaScript object
            // Note: We explicitly set message as enumerable property so JSON.stringify works
            """
            try {
                var json = $errorMessage;
                var error = new Error(json.message);
                Object.assign(error, json);
                Object.defineProperty(error, 'message', { value: json.message, enumerable: true, writable: true });
                var t = $callback(error);
                if (t instanceof Promise) {
                    t.catch(err => { console.log(">>>>> Unexpected exception in Promise: ", err); });
                }
            } catch(err) {
                console.log(">>>>> Unexpected exception in callback: ", err);
            }
            """.trimIndent()
        }
    }
}
