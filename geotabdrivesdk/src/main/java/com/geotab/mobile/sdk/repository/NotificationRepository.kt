package com.geotab.mobile.sdk.repository

import com.geotab.mobile.sdk.models.NativeNotify

interface NotificationRepository {

    fun getJsonNotification(key: Int): String?

    fun getNotification(key: Int): NativeNotify?

    fun persist(notification: NativeNotify)

    fun unPersist(notificationId: String)

    fun getAll(): List<NativeNotify>
}
