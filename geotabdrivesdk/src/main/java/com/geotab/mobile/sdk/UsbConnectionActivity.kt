package com.geotab.mobile.sdk

import android.app.Activity
import android.os.Bundle

class UsbConnectionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTaskRoot) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(launchIntent)
        }
        this.finish()
    }
}
