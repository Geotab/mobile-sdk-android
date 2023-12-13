package com.geotab.mobile.sdk.module.app

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LifecycleOwner
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.app.ForeGroundService.ForegroundBinder
import com.geotab.mobile.sdk.util.FileUtils

typealias LastServerUpdatedCallbackType = (serverAddr: String) -> Unit

class AppModule(
    private val evaluate: (String, (String) -> Unit) -> Unit,
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit,
    moveAppToBackground: () -> Unit
) : Module(MODULE_NAME) {
    private lateinit var adapter: BackgroundModeAdapter
    private lateinit var context: Context
    private lateinit var fileUtils: FileUtils

    // Service that keeps the app awake
    private var foreGroundService: ForeGroundService? = null

    var lastServerUpdatedCallback: LastServerUpdatedCallbackType = {}
    var driveReadyCallback: () -> Unit = {}
    private var keepAlive = "{}"

    init {
        functions.add(UpdateLastServerFunction(module = this))
        functions.add(ClearWebViewCacheFunction(module = this))
        functions.add(MoveAppToBackgroundFunction(moveAppToBackground = moveAppToBackground, module = this))
    }

    companion object {
        const val MODULE_NAME = "app"
    }

    // Used to (un)bind the service to with the activity
    private val foregroundConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundBinder
            this@AppModule.foreGroundService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            push(
                ModuleEvent(
                    "app.background.keepalive",
                    "{ detail:{error : 'service disconnected'}}"
                )
            ) {}
        }
    }

    // Flag indicates if the service is bind
    private var isBound: Boolean = false

    var isBackground: Boolean = false
        private set

    fun initValues(context: Context) {
        initValues(context, FileUtils(context))
    }

    fun initValues(context: Context, fileUtils: FileUtils) {
        this.context = context
        this.fileUtils = fileUtils
        adapter = BackgroundModeAdapterDefault(context as LifecycleOwner)
        startMonitoringBackground()
    }

    private fun startMonitoringBackground() {
        adapter.startMonitoringBackground { onBackgroundModeChange(it) }
    }

    private fun stopMonitoringBackground() {
        adapter.stopMonitoringBackground()
    }

    override fun scripts(context: Context): String {
        var scripts = super.scripts(context)
        scripts += """window.$geotabModules.$name.background = $isBackground;"""
        scripts += """window.$geotabModules.$name.keepAlive = $keepAlive;"""
        return scripts
    }

    private fun onBackgroundModeChange(backgroundMode: BackgroundMode) {
        this.isBackground = backgroundMode.isBackground
        keepAlive = "{}"
        evaluateScript()
        push(ModuleEvent("app.background", "{ detail: $isBackground }")) {}
    }

    fun startForegroundService() {
        if (isBound) return

        val intent = Intent(context, ForeGroundService::class.java)
        try {
            isBound = context.bindService(intent, foregroundConnection, BIND_AUTO_CREATE)
            context.startService(intent)
        } catch (e: Exception) {
            keepAlive = "{ error: \"${e.message}\" }"
            evaluateScript()
            push(ModuleEvent("app.background.keepalive", "{detail: {error: \'${e.message}\'}}")) {}
            stopForegroundService()
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    fun stopForegroundService() {
        stopMonitoringBackground()

        if (!isBound) return
        val intent = Intent(context, ForeGroundService::class.java)
        context.unbindService(foregroundConnection)
        context.stopService(intent)
        isBound = false
    }

    /**
     * Evaluate the scripts again to populate background and keepAlive variables
     */
    private fun evaluateScript() {
        val script =
            """
                if (window.$geotabModules != null && window.$geotabModules.$name != null) { 
                    window.$geotabModules.$name.background = $isBackground; 
                    window.$geotabModules.$name.keepAlive = $keepAlive;
                }
            """.trimMargin()
        evaluate(script) {}
    }

    fun clearCacheDirs(): Boolean {
        // Tried using `WebView.clearCache(true)` in the DriveFragment in order to clear these caches.
        // Using the file explorer, the dates on the folders are getting updated, but could not determine if anything was actually deleted.
        // Service worker related files are not touched as per this bug report
        // https://issuetracker.google.com/issues/37135461
        // Solution is to manually delete the directories
        // Another link on the topic https://stackoverflow.com/questions/47414581/webview-clear-service-worker-cache-programmatically
        return fileUtils.deleteApplicationCacheDir() && fileUtils.deleteServiceWorkerDir()
    }
}
