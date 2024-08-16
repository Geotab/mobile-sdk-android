package com.geotab.mobile.sdk.module.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BackgroundModeAdapterDefault(private val lifecycleOwner: LifecycleOwner) : DefaultLifecycleObserver, BackgroundModeAdapter {
    private var delegate: ((result: BackgroundMode) -> Unit)? = null

    override fun startMonitoringBackground(onBackgroundChange: (result: BackgroundMode) -> Unit) {
        this.delegate = onBackgroundChange
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun stopMonitoringBackground() {
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        delegate?.let { it(BackgroundMode(false)) }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        delegate?.let { it(BackgroundMode(true)) }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        delegate?.let { it(BackgroundMode(false)) }
    }
}
