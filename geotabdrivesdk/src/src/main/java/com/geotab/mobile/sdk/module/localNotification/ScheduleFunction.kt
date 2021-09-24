package com.geotab.mobile.sdk.module.localNotification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.NativeNotify
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.repository.PreferenceNotificationRepository
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ScheduleFunction(
    override val name: String = "schedule",
    override val module: LocalNotificationModule,
    val prefsNotificationRepository: PreferenceNotificationRepository,
    val context: Context
) :
    ModuleFunction, BaseFunction<NativeNotify>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val nativeNotify = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        schedule(nativeNotify, jsCallback) {
            result ->
            jsCallback(Success("$result"))
        }
    }

    override fun getType(): Type {
        return object : TypeToken<NativeNotify>() {}.type
    }

    /**
     * Display the Notification information in the Android notification bar.
     *
     * @param notification Local notification to display.
     * @param onResult callback to pass the result
     */
    private fun schedule(notification: NativeNotify, jsCallback: (Result<Success<String>, Failure>) -> Unit, onResult: (Boolean) -> Unit) {
        val notificationBuilderProvider = NotificationBuilderProvider(context)
        val notificationBuilder = with(notificationBuilderProvider.getBuilder()) {
            setContentTitle(notification.title)
            setContentText(notification.text)
            setPriority(notification.priority ?: NotificationCompat.PRIORITY_DEFAULT)
        }
        try {
            notificationBuilder.setStyle(notificationBuilderProvider.getStyle(notificationBuilder, notification.notificationText))

            for (action in notification.actions.orEmpty()) {
                val btn = NotificationCompat.Action.Builder(
                    0,
                    action.title,
                    notificationBuilderProvider.getPendingIntentForAction(action, notification.notificationID)
                )
                notificationBuilder.addAction(btn.build())
            }

            notificationBuilder.setContentIntent(notificationBuilderProvider.getPendingContentIntent(notification.notificationID))
            notificationBuilder.setDeleteIntent(notificationBuilderProvider.getPendingDeleteIntent(notification.notificationID))
            module.showNotification(notification.notificationID, notificationBuilder.build())
            prefsNotificationRepository.persist(notification)
            onResult(module.checkPermission())
        } catch (exception: Exception) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_NOTIFICATION_ERROR, exception.message)))
        }
    }
}
