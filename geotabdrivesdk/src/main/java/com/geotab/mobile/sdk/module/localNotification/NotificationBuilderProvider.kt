package com.geotab.mobile.sdk.module.localNotification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.geotab.mobile.sdk.NotificationActivity
import com.geotab.mobile.sdk.R
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
            setColor(getNotificationColor())
        }
    }

    /**
     * Get notification icon background color from app's theme.
     * Creates a themed context wrapper using the app's theme resource, then resolves
     * colorPrimaryDark from that themed context.
     * Falls back to whitelabel_background_color if resolution fails.
     * @return Int color value
     */
    private fun getNotificationColor(): Int {
        return try {
            resolveColorPrimaryDarkFromTheme() ?: getFallbackColor()
        } catch (_: Exception) {
            getFallbackColor()
        }
    }

    /**
     * Attempts to resolve colorPrimaryDark from the app's theme.
     * @return Resolved color value, or null if resolution fails
     */
    private fun resolveColorPrimaryDarkFromTheme(): Int? {
        val appThemeResId = getAppThemeResourceId()
        if (appThemeResId == 0) return null

        val themedContext = ContextThemeWrapper(context, appThemeResId)
        val typedValue = android.util.TypedValue()

        val resolved = themedContext.theme.resolveAttribute(
            androidx.appcompat.R.attr.colorPrimaryDark,
            typedValue,
            true
        )

        return if (resolved && typedValue.resourceId != 0) {
            ContextCompat.getColor(context, typedValue.resourceId)
        } else {
            null
        }
    }

    /**
     * Returns the fallback notification color when theme resolution fails.
     * @return Default whitelabel background color
     */
    private fun getFallbackColor(): Int {
        return ContextCompat.getColor(context, R.color.whitelabel_background_color)
    }

    /**
     * Get the app's theme resource ID by looking up the "AppTheme" style resource
     * @return Int theme resource ID, or 0 if not found
     */
    @Suppress("DiscouragedApi")
    private fun getAppThemeResourceId(): Int {
        return try {
            context.resources.getIdentifier("AppTheme", "style", context.packageName)
        } catch (e: Exception) {
            0
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
        val actionIntent = Intent(context, NotificationActivity::class.java)
            .putExtra(LocalNotificationModule.NOTIFICATION_ID, notificationId)
            .putExtra(LocalNotificationModule.NOTIFICATION_ACTION_ID, action.id)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val random = SecureRandom()
        val reqCode = random.nextInt()
        return PendingIntent.getActivity(context, reqCode, actionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Set Intent to handle the click event. Will bring the app to foreground
     * @param notificationId Notification Id
     * @return PendingIntent
     */
    fun getPendingContentIntent(notificationId: Int): PendingIntent {
        val contentIntent = Intent(context, NotificationActivity::class.java)
            .putExtra(LocalNotificationModule.NOTIFICATION_ID, notificationId)
            .putExtra(LocalNotificationModule.NOTIFICATION_ACTION_ID, LocalNotificationModule.CLICK_ACTION_ID)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val random = SecureRandom()
        val reqCode = random.nextInt()
        return PendingIntent.getActivity(context, reqCode, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
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
