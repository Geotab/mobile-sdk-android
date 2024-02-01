package com.geotab.mobile.sdk.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

// remove this after moving to API 34, currently VERSION_CODES.UPSIDE_DOWN_CAKE throws an
// unresolved reference error
const val UPSIDE_DOWN_CAKE = 34

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= TIRAMISU -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun PackageManager.installedPackages(flag: Int): List<PackageInfo> = when {
    SDK_INT >= TIRAMISU -> getInstalledPackages(PackageManager.PackageInfoFlags.of(flag.toLong()))
    else -> @Suppress("DEPRECATION") getInstalledPackages(flag)
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    // need to check for API 34 here since the getXXX functions are unsafe on API 33
    SDK_INT >= UPSIDE_DOWN_CAKE -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}
