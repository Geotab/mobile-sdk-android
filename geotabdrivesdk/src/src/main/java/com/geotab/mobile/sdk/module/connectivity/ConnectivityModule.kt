package com.geotab.mobile.sdk.module.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.geotab.mobile.sdk.models.ConnectivityState
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.util.JsonUtil

class ConnectivityModule(
    val context: Context,
    private val evaluate: (String, (String) -> Unit) -> Unit,
    private val push: (ModuleEvent) -> Unit,
    override val name: String = "connectivity"
) : Module(name) {
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

    override fun scripts(context: Context): String {
        var scripts = super.scripts(context)
        val type = getNetworkType()

        stateJson(type != ConnectivityType.NONE && type != ConnectivityType.UNKNOWN).let {
            scripts += """window.$geotabModules.$name.state = $it"""
        }
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

    private fun updateConnectionInfo(online: Boolean) {
        updateState(online)
        signalConnectivityEvent(online)
    }

    private fun updateState(online: Boolean) {
        stateJson(online).let {
            val script = """window.$geotabModules.$name.state = $it"""
            evaluate(script) {}
        }
    }

    private fun signalConnectivityEvent(online: Boolean) {
        stateJson(online).let {
            push(
                ModuleEvent(
                    "connectivity",
                    "{ detail: $it }"
                )
            )
        }
    }

    private fun stateJson(online: Boolean): String {
        val state = ConnectivityState(online, getNetworkType().toString())
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
