package com.geotab.mobile.sdk.module.localNotification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.geotab.mobile.sdk.models.NativeNotifyAction
import java.security.SecureRandom

/**
 * Helper class to build fully configured local notification specified by the native object
 * @property context Application context
 * @constructor
 */
class NotificationBuilderProvider(val context: Context) {

    fun getBuilder(): NotificationCompat.Builder {
        return with(NotificationCompat.Builder(context, LocalNotificationModule.CHANNEL_ID)) {
            setDefaults(Notification.DEFAULT_ALL)
            setOnlyAlertOnce(false)
            setAutoCancel(true)
            setOngoing(false)
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            setSmallIcon(getSmallIcon())
        }
    }

    /**
     * Small icon resource ID for the local notification
     * @return Int
     */
    private fun getSmallIcon(): Int {
        val metadata = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
        return metadata.getInt("default_notification_icon", android.R.drawable.ic_popup_reminder)
    }
    /**
     * Set style BigTextStyle by default.
     * @param notificationBuilder Local notification builder instance.
     * @param notificationText Text for local notification
     * @return NotificationCompat.BigTextStyle
     */
    fun getStyle(notificationBuilder: NotificationCompat.Builder, notificationText: String): NotificationCompat.BigTextStyle =
        NotificationCompat.BigTextStyle(notificationBuilder)
            .bigText(notificationText)

    /**
     *  Returns a new PendingIntent for a notification action, including the action's identifier.
     * @param action Notification action which requires the PendingIntent
     * @param notificationId Notification identifier
     * @return PendingIntent
     */
    fun getPendingIntentForAction(action: NativeNotifyAction, notificationId: Int): PendingIntent {
        val actionIntent = Intent(context, ClickNotificationReceiver::class.java)
            .putExtra(LocalNotificationModule.NOTIFICATION_ID, notificationId)
            .putExtra(LocalNotificationModule.NOTIFICATION_ACTION_ID, action.id)
        val random = SecureRandom()
        val reqCode = random.nextInt()
        return PendingIntent.getBroadcast(context, reqCode, actionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Set Intent to handle the click event. Will bring the app to foreground
     * @param notificationId Notification Id
     * @return PendingIntent
     */
    fun getPendingContentIntent(notificationId: Int): PendingIntent {
        val contentIntent = Intent(context, ClickNotificationReceiver::class.java)
            .putExtra(LocalNotificationModule.NOTIFICATION_ID, notificationId)
            .putExtra(LocalNotificationModule.NOTIFICATION_ACTION_ID, LocalNotificationModule.CLICK_ACTION_ID)
        val random = SecureRandom()
        val reqCode = random.nextInt()
        return PendingIntent.getBroadcast(context, reqCode, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Set Intent to handle the delete event. Will clean up persisted preferences.
     * @param notificationId Notification Id
     * @return PendingIntent
     */
    fun getPendingDeleteIntent(notificationId: Int): PendingIntent {
        val deleteIntent = Intent(context, ClearNotificationReceiver::class.java)
            .setAction(notificationId.toString())
            .putExtra(LocalNotificationModule.NOTIFICATION_ID, notificationId)
        val random = SecureRandom()
        val reqCode = random.nextInt()
        return PendingIntent.getBroadcast(
            context, reqCode, deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
