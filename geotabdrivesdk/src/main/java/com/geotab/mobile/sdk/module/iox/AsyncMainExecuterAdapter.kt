package com.geotab.mobile.sdk.module.iox

interface AsyncMainExecuterAdapter {
    fun after(seconds: Int, execute: () -> Unit)
}
