package com.geotab.mobile.sdk

import android.app.Activity
import android.os.Bundle

/**
 *  An Activity to open up on clicking the notification and closes itself.
 *  This is added to resume to the existing stack.
 */
class NotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.finish()
    }
}
