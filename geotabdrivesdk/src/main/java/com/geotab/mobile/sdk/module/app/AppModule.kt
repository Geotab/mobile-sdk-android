package com.geotab.mobile.sdk.module.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LifecycleOwner
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.app.ForeGroundService.ForegroundBinder
typealias LastServerUpdatedCallbackType = (serverAddr: String) -> Unit

class AppModule(
    private val evaluate: (String, (String) -> Unit) -> Unit,
    private val push: (ModuleEvent) -> Unit,
    override val name: String = "app"
) : Module(name) {
    private lateinit var adapter: BackgroundModeAdapter
    private lateinit var context: Context
    // Service that keeps the app awake
    private var foreGroundService: ForeGroundService? = null

    var lastServerUpdatedCallback: LastServerUpdatedCallbackType = {}

    init {
        functions.add(UpdateLastServerFunction(module = this))
    }

    // Used to (un)bind the service to with the activity
    private val foreGroundconnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundBinder
            this@AppModule.foreGroundService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            push(ModuleEvent("app.background.keepalive", "{ detail:{error : 'service disconnected'}}"))
        }
    }

    // Flag indicates if the service is bind
    private var isBound: Boolean = false

    var isBackground: Boolean = false
        private set

    fun initValues(context: Context) {
        this.context = context
        adapter = BackgroundModeAdapterDefault(context as LifecycleOwner)
    }

    fun startMonitoringBackground() {
        adapter.startMonitoringBackground { onBackgroundModeChange(it) }
    }

    fun stopMonitoringBackground() {
        adapter.stopMonitoringBackground()
    }

    override fun scripts(context: Context): String {
        var scripts = super.scripts(context)
        scripts += "window.$geotabModules.$name.background = $isBackground;"
        return scripts
    }

    private fun isFinishing(): Boolean {
        return ((context as Activity).isFinishing)
    }

    private fun onBackgroundModeChange(backgroundMode: BackgroundMode) {
        // While the activity is destroyed, don't invoke background change or the service
        if (isFinishing()) {
            // To avoid memory leak, stopservice if the app in the background is killed by the OS
            if (isBound) {
                stopService()
            }
            return
        }
        this.isBackground = backgroundMode.isBackground
        val script = "if (window.$geotabModules != null) { window.$geotabModules.$name.background = $isBackground; }"
        evaluate(script) {}
        push(ModuleEvent("app.background", "undefined"))

        if (isBackground) startService() else stopService()
    }

    private fun startService() {
        val intent = Intent(context, ForeGroundService::class.java)
        if (isBound) return
        try {
            isBound = context.bindService(intent, foreGroundconnection, BIND_AUTO_CREATE)
            context.startService(intent)
        } catch (e: Exception) {
            push(ModuleEvent("app.background.keepalive", "{detail: {error: \'${e.message}\'}}"))
            stopService()
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private fun stopService() {
        if (!isBound) return
        val intent = Intent(context, ForeGroundService::class.java)
        context.unbindService(foreGroundconnection)
        context.stopService(intent)
        isBound = false
    }
}
