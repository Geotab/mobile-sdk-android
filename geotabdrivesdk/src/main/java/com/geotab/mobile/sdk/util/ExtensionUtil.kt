package com.geotab.mobile.sdk.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
    IntentCompat.getParcelableArrayListExtra(this, key, T::class.java)

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? =
    BundleCompat.getParcelableArrayList(this, key, T::class.java)

inline fun PackageManager.installedPackages(flag: Int): List<PackageInfo> = when {
    SDK_INT >= TIRAMISU -> getInstalledPackages(PackageManager.PackageInfoFlags.of(flag.toLong()))
    else -> @Suppress("DEPRECATION") getInstalledPackages(flag)
}

inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? = when {
    SDK_INT >= UPSIDE_DOWN_CAKE -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    // need to check for API 34 here since the getXXX functions are unsafe on API 33
    SDK_INT >= UPSIDE_DOWN_CAKE -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.regReceiver(broadcastReceiver: BroadcastReceiver?, intentFilter: IntentFilter, exported: Boolean = false): Intent? {
    return if (SDK_INT >= UPSIDE_DOWN_CAKE) {
        if (exported) {
            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        }
    } else {
        registerReceiver(broadcastReceiver, intentFilter)
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
inline fun BluetoothGattServer.callNotifyCharacteristicChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic, confirm: Boolean, value: ByteArray): Any = when {
    SDK_INT >= TIRAMISU -> {
        this.notifyCharacteristicChanged(device, characteristic, confirm, value)
    }
    else -> {
        characteristic.value = value
        this.notifyCharacteristicChanged(device, characteristic, confirm)
    }
}
