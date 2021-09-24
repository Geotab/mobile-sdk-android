package com.geotab.mobile.sdk.module.motion

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.geotab.mobile.sdk.models.enums.MotionEnum
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class MotionActivityAdapterDefault(private val context: Context) : MotionActivityAdapter,
    BroadcastReceiver() {
    private var delegate: ((result: MotionEnum) -> Unit)? = null
    private val adapterRegistrationName =
        context.applicationContext.packageName + ".MotionActivityAdapter"

    private var isMotionActivityRegistered = false

    // The reason of the suppress here is because we removed
    // the permission from our AndroidManifest to let the
    // implementer do it on their AndroidManifest
    @Suppress("MissingPermission")
    override fun startMonitoringMotionActivity(
        onMotionActivityChange: (result: MotionEnum) -> Unit,
        startCallback: (Boolean, Boolean) -> Unit
    ) {
        this.delegate = onMotionActivityChange

        val transitions = getActivitiesMonitorList()

        val intent = Intent(adapterRegistrationName)
        val request = ActivityTransitionRequest(transitions)

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        val task = ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            isMotionActivityRegistered = true
        }

        task.addOnFailureListener {
            isMotionActivityRegistered = false
        }

        context.registerReceiver(
            this,
            IntentFilter(adapterRegistrationName)
        )

        // First true means the user granted permission. Second true
        // means the service was started
        startCallback(true, true)
    }

    override fun stopMonitoringMotionActivity() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                for (event in it.transitionEvents) {
                    val activity = activityType(event.activityType)
                    delegate?.let { it(activity) }
                }
            }
        }
    }

    private fun activityType(activity: Int): MotionEnum {
        return when (activity) {
            DetectedActivity.IN_VEHICLE -> MotionEnum.DRIVING
            DetectedActivity.STILL -> MotionEnum.STATIONARY
            DetectedActivity.WALKING -> MotionEnum.WALKING
            DetectedActivity.ON_BICYCLE -> MotionEnum.BIKING
            DetectedActivity.RUNNING -> MotionEnum.RUNNING
            else -> MotionEnum.UNKNOWN
        }
    }

    private fun getActivitiesMonitorList(): MutableList<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        return transitions
    }
}
