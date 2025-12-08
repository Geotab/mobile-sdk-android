package com.geotab.mobile.sdk

import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.geotab.mobile.sdk.logging.Logger

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
}
