package com.geotab.mobile.sdk.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= TIRAMISU -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun PackageManager.installedPackages(flag: Int): List<PackageInfo> = when {
    SDK_INT >= TIRAMISU -> getInstalledPackages(PackageManager.PackageInfoFlags.of(flag.toLong()))
    else -> @Suppress("DEPRECATION") getInstalledPackages(flag)
}
