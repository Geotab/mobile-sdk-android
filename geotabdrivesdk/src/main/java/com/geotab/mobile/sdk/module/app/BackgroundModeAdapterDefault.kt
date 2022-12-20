package com.geotab.mobile.sdk.module.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class BackgroundModeAdapterDefault(private val lifecycleOwner: LifecycleOwner) : LifecycleObserver, BackgroundModeAdapter {
    private var delegate: ((result: BackgroundMode) -> Unit)? = null

    override fun startMonitoringBackground(onBackgroundChange: (result: BackgroundMode) -> Unit) {
        this.delegate = onBackgroundChange
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun stopMonitoringBackground() {
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroy() {
        delegate?.let { it(BackgroundMode(false)) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppBackgrounded() {
        delegate?.let { it(BackgroundMode(true)) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        delegate?.let { it(BackgroundMode(false)) }
    }
}
