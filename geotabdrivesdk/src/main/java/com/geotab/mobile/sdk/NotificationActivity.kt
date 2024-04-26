package com.geotab.mobile.sdk

import android.app.Activity
import android.os.Bundle
import com.geotab.mobile.sdk.logging.InternalAppLogging
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.localNotification.LocalNotificationModule

/**
 *  An Activity to open up on clicking the notification and closes itself.
 *  This is added to resume to the existing stack.
 */
class NotificationActivity : Activity() {

    companion object {
        private const val TAG = "NotificationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationId = intent.getIntExtra(
            LocalNotificationModule.NOTIFICATION_ID,
            LocalNotificationModule.NOTIFICATION_ID_DEFAULT_VALUE
        )

        val action = intent.getStringExtra(
            LocalNotificationModule.NOTIFICATION_ACTION_ID
        ) ?: LocalNotificationModule.CLICK_ACTION_ID

        try {
            LocalNotificationModule.UserActionNotification.fireEvent(
                action,
                notificationId,
                this,
                isTaskRoot
            )
        } catch (e: Exception) {
            Logger.shared.debug(TAG, e.message ?: "Error in NotificationActivity")
            InternalAppLogging.appLogger?.error(TAG, "Error in UserActionNotification.fireEvent: ${e.message ?: "Unknown error"}")
        }

        this.finish()
    }
}
