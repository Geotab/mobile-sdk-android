package com.geotab.mobile.sdk.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.geotab.mobile.sdk.models.NativeNotify
import com.geotab.mobile.sdk.util.JsonUtil
import com.google.gson.JsonSyntaxException

class PreferenceNotificationRepository(val context: Context, val name: String) : NotificationRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    override fun getJsonNotification(key: Int): String? {
        return prefs.getString(key.toString(), null)
    }

    override fun getNotification(key: Int): NativeNotify? {
        return try {
            prefs.getString(key.toString(), null) ?.let {
                JsonUtil.fromJson<NativeNotify>(it)
            }
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Persist the information of this notification as Json string, to the Android shared
     * preferences. This will allow the application to retrieve the notifications.
     *
     * @param notification Local notification to save
     */
    override fun persist(notification: NativeNotify) {
        prefs.edit {
            putString(
                notification.id.toString(),
                JsonUtil.toJson(notification)
            )
        }
    }

    /**
     * Remove the notification from the Android shared preferences.
     * @param notificationId Local notification id to delete.
     */
    override fun unPersist(notificationId: String) {
        prefs.edit {
            remove(notificationId)
        }
    }

    /**
     * Get all notification ids
     * @return List<Int>
     */
    private fun getIds(): List<Int> {
        return try {
            prefs.all.keys.map {
                it.toInt()
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            listOf()
        }
    }

    /**
     * List of all local notification
     * @return List<NativeNotify>
     */
    override fun getAll(): List<NativeNotify> {
        return getIds().mapNotNull {
            getNotification(it)
        }
    }
}
