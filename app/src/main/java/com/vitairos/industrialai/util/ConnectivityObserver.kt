package com.vitairos.industrialai.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Emite true/false según haya una red con acceso a Internet. */
class ConnectivityObserver(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    fun isOnlineNow(): Boolean {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    val online: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(isOnlineNow()) }
        }
        trySend(isOnlineNow())
        cm?.registerDefaultNetworkCallback(callback)
        awaitClose { cm?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
