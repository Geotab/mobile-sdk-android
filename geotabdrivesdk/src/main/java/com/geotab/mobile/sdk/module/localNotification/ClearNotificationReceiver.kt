package com.geotab.mobile.sdk.module.localNotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geotab.mobile.sdk.logging.InternalAppLogging
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.localNotification.LocalNotificationModule.Companion.CANCEL_NOTIFICATION_ID

/**
 * Delete receiver for local notification. Creates the local notification and calls the event
 * functions for further proceeding.
 */
class ClearNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ClearNotificationReceiver"
    }

    /**
     * Called when the notification was cleared from the notification center.
     *
     * @param context Application context.
     * @param intent Received intent with content data.
     */
    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.getInt(LocalNotificationModule.NOTIFICATION_ID)?.let { notificationId ->
            try {
                LocalNotificationModule.UserActionNotification.fireEvent(CANCEL_NOTIFICATION_ID, notificationId, context)
            } catch (e: Exception) {
                Logger.shared.debug(TAG, e.message ?: "Error in ClearNotificationReceiver")
                InternalAppLogging.appLogger?.error(TAG, "Error in UserActionNotification.fireEvent: ${e.message ?: "Unknown error"}")
            }
        }
    }
}
