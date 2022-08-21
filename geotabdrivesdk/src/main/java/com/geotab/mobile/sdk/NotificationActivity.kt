package com.geotab.mobile.sdk

import android.app.Activity
import android.os.Bundle
import com.geotab.mobile.sdk.module.localNotification.LocalNotificationModule

/**
 *  An Activity to open up on clicking the notification and closes itself.
 *  This is added to resume to the existing stack.
 */
class NotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationId = intent.getIntExtra(
            LocalNotificationModule.NOTIFICATION_ID,
            LocalNotificationModule.NOTIFICATION_ID_DEFAULT_VALUE
        )

        val action = intent.getStringExtra(
            LocalNotificationModule.NOTIFICATION_ACTION_ID
        ) ?: LocalNotificationModule.CLICK_ACTION_ID

        LocalNotificationModule.UserActionNotification.fireEvent(
            action,
            notificationId,
            this,
            isTaskRoot
        )

        this.finish()
    }
}
