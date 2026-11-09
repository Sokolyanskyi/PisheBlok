package com.pisheblok.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Observes Internet connectivity so the sync can be retried when the network returns. */
class NetworkObserver(context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(isOnline())
    val online: StateFlow<Boolean> = _online

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _online.value = true
                }

                override fun onLost(network: Network) {
                    _online.value = isOnline()
                }
            })
        } catch (_: Exception) {
            // connectivity monitoring is best-effort
        }
    }

    private fun isOnline(): Boolean = cm.activeNetwork?.let { network ->
        cm.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } ?: false
}
