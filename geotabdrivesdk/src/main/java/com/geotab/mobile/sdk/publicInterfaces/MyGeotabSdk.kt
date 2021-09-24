package com.geotab.mobile.sdk.publicInterfaces

import com.geotab.mobile.sdk.module.app.LastServerUpdatedCallbackType

interface MyGeotabSdk {
    /**
     * Callback to inform that the Web application can't be loaded, due to network failure.
     */
    var webAppLoadFailed: (() -> Unit)?

    /**
     * Set a callback to listen for "last server address" change event.
     * LastServerUpdatedCallbackType is a function with the new server address as the argument.
     */
    fun setLastServerAddressUpdatedCallback(callback: LastServerUpdatedCallbackType)

    /**
     * Clears the previously set "last server address" callback
     */
    fun clearLastServerAddressUpdatedCallback()
}
