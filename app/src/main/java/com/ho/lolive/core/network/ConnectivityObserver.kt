package com.ho.lolive.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observe(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasUsableConnection(cm))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(hasUsableConnection(cm))
            }

            override fun onLost(network: Network) {
                trySend(hasUsableConnection(cm))
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        trySend(hasUsableConnection(cm))
        cm.registerNetworkCallback(request, callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun hasUsableConnection(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        // 只要求具备上网能力即可。不要强制 VALIDATED：
        // 部分 OEM / 代理 / 校园网场景下 VALIDATED 会长期为 false，但流媒体实际可播。
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
