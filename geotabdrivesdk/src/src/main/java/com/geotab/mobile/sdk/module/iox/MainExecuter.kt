package com.geotab.mobile.sdk.module.iox

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainExecuter : AsyncMainExecuterAdapter {
    override fun after(seconds: Int, execute: () -> Unit) {
        MainScope().launch {
            delay(seconds * 1000L)
            execute()
        }
    }
}
