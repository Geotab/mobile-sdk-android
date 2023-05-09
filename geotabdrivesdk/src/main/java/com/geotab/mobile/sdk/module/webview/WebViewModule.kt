package com.geotab.mobile.sdk.module.webview

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.geotab.mobile.sdk.module.Module

class WebViewModule(
    activity: FragmentActivity,
    private val goBack: () -> Unit
) : Module(MODULE_NAME) {

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    init {
        activity.onBackPressedDispatcher.addCallback(callback)
    }

    companion object {
        const val MODULE_NAME = "webview"
    }
}
