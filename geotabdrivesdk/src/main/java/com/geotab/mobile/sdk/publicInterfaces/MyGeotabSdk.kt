package com.geotab.mobile.sdk.publicInterfaces

interface MyGeotabSdk {
    /**
     * Callback to inform that the Web application can't be loaded, due to network failure.
     */
    var webAppLoadFailed: (() -> Unit)?
}
