package com.geotab.mobile.sdk.module.localNotification

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
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
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.repository.PreferenceNotificationRepository
import com.geotab.mobile.sdk.util.JsonUtil

class LocalNotificationModule(
    context: Context,
    permissionDelegate: PermissionDelegate
) : Module(MODULE_NAME) {
    companion object {
        var actionHandler: ((Result<Success<String>, Failure>) -> Unit)? = null
        var actionIdentifier = emptyArray<String>()

        const val CHANNEL_ID = "default-channel-id"
        const val CHANNEL_NAME = "Default channel"
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        const val NOTIFICATION_ID_DEFAULT_VALUE = 0
        const val NOTIFICATION_ACTION_ID = "NOTIFICATION_ACTION_ID"
        const val CLICK_ACTION_ID = "click"
        const val CANCEL_NOTIFICATION_ID = "cancel"
        const val NOTIFICATION_TEXT_NULL = "Notification text cannot be null"
        const val NOTIFICATION_TEXT_EMPTY = "Notification text cannot be empty"
        const val NOTIFICATION_ID_NULL = "Notification ID cannot be null"
        const val MODULE_NAME = "localNotification"

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

    val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
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

    fun requestPermission(callback: (Boolean) -> Unit) {
        permissionHelper.requestPermission(arrayOf(Permission.POST_NOTIFICATIONS), callback)
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
        fun fireEvent(
            action: String,
            notificationId: Int,
            context: Context,
            isRootTask: Boolean = false
        ) {
            val prefsNotificationRepository = PreferenceNotificationRepository(
                context,
                NOTIFICATION_ID
            )
            prefsNotificationRepository.getNotification(
                notificationId
            )?.let {
                launchApp(context, isRootTask)
                fireJSAction(action, it)
                prefsNotificationRepository.unPersist(notificationId.toString())
                getNotificationManager(context).cancel(notificationId)
            }
        }

        /**
         * Launch main intent from package.
         */
        private fun launchApp(context: Context, isRootTask: Boolean) {
            if (!isRootTask) {
                // Now launch the app.
                val activityManager =
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                // if the current application is active, bring it to foreground
                activityManager.appTasks.getOrNull(0)?.moveToFront()
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                context.startActivity(launchIntent)
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
                    it(
                        Failure(
                            Error(
                                GeotabDriveError.MODULE_NOTIFICATION_ERROR,
                                exception.message
                            )
                        )
                    )
                }
            }
        }
    }
}
