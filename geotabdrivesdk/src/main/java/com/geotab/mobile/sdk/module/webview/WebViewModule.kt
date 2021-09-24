package com.geotab.mobile.sdk.module.webview

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.geotab.mobile.sdk.module.Module

class WebViewModule(
    activity: FragmentActivity,
    private val goBack: () -> Unit,
    override val name: String = "webview"
) : Module(name) {

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    init {
        activity.onBackPressedDispatcher.addCallback(callback)
    }
}
