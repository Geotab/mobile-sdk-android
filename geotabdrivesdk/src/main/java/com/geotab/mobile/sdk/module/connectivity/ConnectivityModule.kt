package com.geotab.mobile.sdk.module.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.geotab.mobile.sdk.models.ConnectivityState
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil

class ConnectivityModule(
    val context: Context,
    private val evaluate: (String, (String) -> Unit) -> Unit,
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit
) : Module(MODULE_NAME) {
    var started: Boolean = false

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    private val networkCallback: ConnectivityManager.NetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateConnectionInfo(true)
            }

            override fun onLost(network: Network) {
                updateConnectionInfo(false)
            }
        }
    }

    init {
        functions.add(StartFunction(module = this))
        functions.add(StopFunction(module = this))
    }

    companion object {
        const val MODULE_NAME = "connectivity"
    }

    override fun scripts(context: Context): String {
        var scripts = super.scripts(context)
        val type = getNetworkType()

        val connectivityJson =
            stateJson(type != ConnectivityType.NONE && type != ConnectivityType.UNKNOWN)
        scripts += """window.$geotabModules.$name.state = $connectivityJson;"""
        return scripts
    }

    fun registerConnectivityActionReceiver(): Boolean {
        connectivityManager?.let {
            it.registerDefaultNetworkCallback(networkCallback)
            return true
        }
        return false
    }

    fun unRegisterConnectivityActionReceiver(): Boolean {
        connectivityManager?.let {
            it.unregisterNetworkCallback(networkCallback)
            return true
        }
        return false
    }

    internal fun updateConnectionInfo(online: Boolean) {
        updateState(online)
        signalConnectivityEvent(online)
    }

    private fun updateState(online: Boolean) {
        val script =
            """
                if (window.$geotabModules != null && window.$geotabModules.$name != null) { 
                    window.$geotabModules.$name.state = ${stateJson(online)}
                } 
            """.trimMargin()
        evaluate(script) {}
    }

    private fun signalConnectivityEvent(online: Boolean) {
        push(
            ModuleEvent(
                "connectivity",
                "{ detail: ${stateJson(online)} }"
            )
        ) {}
    }

    private fun stateJson(online: Boolean): String {
        val networkType = if (online) {
            getNetworkType()
        } else {
            ConnectivityType.NONE
        }
        val state = ConnectivityState(online, networkType.toString())
        return JsonUtil.toJson(state)
    }

    private fun getNetworkType(): ConnectivityType {
        connectivityManager?.let { cm ->
            cm.getNetworkCapabilities(cm.activeNetwork)?.let { nwCapabilities ->
                when {
                    nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return ConnectivityType.CELL
                    nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return ConnectivityType.WIFI
                    nwCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return ConnectivityType.ETHERNET
                }
                return ConnectivityType.UNKNOWN
            }
        }
        return ConnectivityType.NONE
    }
}
