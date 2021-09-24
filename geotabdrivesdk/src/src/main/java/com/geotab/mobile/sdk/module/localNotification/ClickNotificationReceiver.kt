package com.geotab.mobile.sdk.module.localNotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Content receiver for local notifications. Creates the local notification and calls the event
 * functions for further proceeding.
 */
class ClickNotificationReceiver : BroadcastReceiver() {

    /**
     * Called when local notification was clicked to send the response.
     *
     * @param context Application context.
     * @param intent Received intent with content data.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val notificationId = bundle.getInt(LocalNotificationModule.NOTIFICATION_ID)
        val action = bundle.getString(
            LocalNotificationModule.NOTIFICATION_ACTION_ID,
            LocalNotificationModule.CLICK_ACTION_ID
        )
        LocalNotificationModule.UserActionNotification.fireEvent(
            action,
            notificationId,
            context
        )
    }
}
