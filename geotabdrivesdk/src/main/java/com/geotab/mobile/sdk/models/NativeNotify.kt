package com.geotab.mobile.sdk.models

import com.geotab.mobile.sdk.module.localNotification.LocalNotificationModule

data class NativeNotify(
    val id: Int?,
    val text: String?,
    val title: String?,
    val icon: String?,
    val smallIcon: String?,
    val priority: Int?,
    val actions: MutableList<NativeNotifyAction>?
) {
    val notificationText
        get() = if (text == null) throw Exception(LocalNotificationModule.NOTIFICATION_TEXT_NULL) else if (text.isEmpty()) throw Exception(LocalNotificationModule.NOTIFICATION_TEXT_EMPTY) else text
    val notificationID
        get() = id ?: throw Exception(LocalNotificationModule.NOTIFICATION_ID_NULL)
}
