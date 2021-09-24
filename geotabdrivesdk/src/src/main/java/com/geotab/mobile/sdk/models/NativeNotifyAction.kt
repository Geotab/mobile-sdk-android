package com.geotab.mobile.sdk.models

data class NativeNotifyAction(
    val id: String,
    val title: String,
    val type: String?, // button or input
    val launch: Boolean,
    val ui: String? // decline
)
