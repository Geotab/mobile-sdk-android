package com.geotab.mobile.sdk.models

data class NativeActionEvent(
    val event: String,
    val foreground: Boolean,
    val notification: Int,
    val queued: Boolean
)
