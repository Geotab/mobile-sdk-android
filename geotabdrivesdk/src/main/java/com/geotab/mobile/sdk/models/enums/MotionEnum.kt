package com.geotab.mobile.sdk.models.enums

enum class MotionEnum(val motionId: Short) {
    UNKNOWN(0),
    STATIONARY(1),
    WALKING(2),
    RUNNING(3),
    BIKING(4),
    DRIVING(5)
}
