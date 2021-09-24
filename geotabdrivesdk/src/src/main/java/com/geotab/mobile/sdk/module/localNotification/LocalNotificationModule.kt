package com.geotab.mobile.sdk.module.localNotification

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.NativeActionEvent
import com.geotab.mobile.sdk.models.NativeNotify
import com.geotab.mobile.sdk.models.NativeNotifyEventResult
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.repository.PreferenceNotificationRepository
import com.geotab.mobile.sdk.util.JsonUtil

class LocalNotificationModule(context: Context, override val name: String = "localNotification") :
    Module(name) {
    companion object {
        var actionHandler: ((Result<Success<String>, Failure>) -> Unit)? = null
        var actionIdentifier = emptyArray<String>()

        const val CHANNEL_ID: String = "default-channel-id"
        const val CHANNEL_NAME: String = "Default channel"
        const val NOTIFICATION_ID: String = "NOTIFICATION_ID"
        const val NOTIFICATION_ACTION_ID: String = "NOTIFICATION_ACTION_ID"
        const val CLICK_ACTION_ID: String = "click"
        const val CANCEL_NOTIFICATION_ID: String = "cancel"
        const val NOTIFICATION_TEXT_NULL = "Notification text cannot be null"
        const val NOTIFICATION_TEXT_EMPTY = "Notification text cannot be empty"
        const val NOTIFICATION_ID_NULL = "Notification ID cannot be null"

        fun getNotificationManager(context: Context): NotificationManager {
            return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    private val notificationManager: NotificationManager = getNotificationManager(context)

    private val prefsNotificationRepository: PreferenceNotificationRepository by lazy {
        PreferenceNotificationRepository(
            context,
            NOTIFICATION_ID
        )
    }

    init {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        functions.add(HasPermissionFunction(module = this))
        functions.add(OnFunction(module = this))
        functions.add(RequestPermissionFunction(module = this))
        functions.add(
            GetAllFunction(
                module = this,
                prefsNotificationRepository = prefsNotificationRepository
            )
        )
        functions.add(OffFunction(module = this))
        functions.add(CancelFunction(module = this))
        functions.add(
            ScheduleFunction(
                module = this,
                prefsNotificationRepository = prefsNotificationRepository,
                context = context
            )
        )
    }

    fun checkPermission(): Boolean {
        return this.notificationManager.areNotificationsEnabled()
    }

    fun showNotification(id: Int, notification: Notification) =
        this.notificationManager.notify(id, notification)

    fun cancelNotification(id: Int): String? {
        prefsNotificationRepository.getJsonNotification(id)?.let { jsonString ->
            this.notificationManager.cancel(id)
            prefsNotificationRepository.unPersist(id.toString())
            return jsonString
        }
        return null
    }

    object UserActionNotification {
        /**
         * Fire the event on JS side. Informs to all the listeners of this event.
         * @param action Action to perform
         * @param notificationId Local notification id
         * @param context Application context
         */
        fun fireEvent(action: String, notificationId: Int, context: Context) {
            val prefsNotificationRepository = PreferenceNotificationRepository(
                context,
                NOTIFICATION_ID
            )
            prefsNotificationRepository.getNotification(
                notificationId
            )?.let {
                launchApp(context)
                fireJSAction(action, it)
                prefsNotificationRepository.unPersist(notificationId.toString())
                getNotificationManager(context).cancel(notificationId)
            }
        }

        /**
         * Launch main intent from package.
         */
        private fun launchApp(context: Context) {
            // Need to send a broadcast to close the notification drawer.
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(it)
            // Now launch the app.
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            if (activityManager.appTasks.size > 0) {
                // if the current application is active, bring it to foreground
                activityManager.appTasks[0].moveToFront()
            } else {
                // If current application is not active, launch the app
                val launchIntent = Intent(Intent.ACTION_MAIN)
                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)

                val targetIntent = context
                    .packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(targetIntent)
            }
        }

        /**
         * Fire the event on JS side. Informs to all the listeners of this event.
         * @param action Action to perform
         * @param notification Local notification
         */
        private fun fireJSAction(action: String, notification: NativeNotify) {
            try {
                val actionStr = actionIdentifier.firstOrNull { identifier -> identifier == action }
                actionStr?.let {
                    val nativeActionEvent = NativeActionEvent(actionStr, true, notification.notificationID, false)
                    val nativeNotifyEventResult =
                        NativeNotifyEventResult(notification, nativeActionEvent)
                    actionHandler?.let {
                        it(Success(JsonUtil.toJson(nativeNotifyEventResult)))
                    }
                }
            } catch (exception: Exception) {
                actionHandler?.let {
                    it(Failure(Error(GeotabDriveError.MODULE_NOTIFICATION_ERROR, exception.message)))
                }
            }
        }
    }
}
