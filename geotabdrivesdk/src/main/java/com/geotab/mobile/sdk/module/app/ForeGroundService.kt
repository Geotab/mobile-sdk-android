package com.geotab.mobile.sdk.module.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.geotab.mobile.sdk.NotificationActivity
import com.geotab.mobile.sdk.R
import com.geotab.mobile.sdk.models.BackgroundNotification
import com.geotab.mobile.sdk.module.localNotification.NotificationBuilderProvider

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
// Fixed ID for the 'foreground' notification
private const val NOTIFICATION_ID = 101
class ForeGroundService : Service() {
    // Binder given to clients
    private val binder: IBinder = ForegroundBinder()

    // Partial wake lock to prevent the app from going to sleep when locked
    private var wakeLock: WakeLock? = null

    inner class ForegroundBinder : Binder() {
        // Return this instance of ForegroundService
        // so clients can call public methods
        val service: ForeGroundService
            get() = this@ForeGroundService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        awakeRelease()
    }

    /**
     * Prevent Android from stopping the background service automatically.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        keepAwake()
        return START_STICKY
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    private fun keepAwake() {
        val settings = BackgroundNotification(
            title = applicationContext.packageManager.getApplicationLabel(applicationContext.applicationInfo).toString(),
            text = applicationContext.getString(R.string.bgNotificationText)
        )
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, makeNotification(settings), FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, makeNotification(settings))
        }
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PARTIAL_WAKE_LOCK, "backgroundmode:wakelock").apply { acquire() }
        }
    }

    /**
     * Stop background mode.
     */
    private fun awakeRelease() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        getNotificationManager(applicationContext).cancel(NOTIFICATION_ID)
        wakeLock?.release()
        wakeLock = null
    }

    private fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @param settings The config settings
     */
    private fun makeNotification(settings: BackgroundNotification): Notification {
        val context = applicationContext

        val notificationBuilder: NotificationCompat.Builder = NotificationBuilderProvider(context).getBuilder()
            .setContentTitle(settings.title)
            .setContentText(settings.text)
            .setOngoing(true)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_MIN)

        // calling a notificationActivity to resume app in it's previous state
        val toLaunch = Intent(context, NotificationActivity::class.java)
        toLaunch.action = "android.intent.action.MAIN"
        toLaunch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val resultPendingIntent = PendingIntent.getActivity(
            context,
            0,
            toLaunch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.setContentIntent(resultPendingIntent)
        return notificationBuilder.build()
    }
}
