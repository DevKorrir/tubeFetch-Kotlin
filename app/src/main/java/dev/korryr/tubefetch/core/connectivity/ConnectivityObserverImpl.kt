package dev.korryr.tubefetch.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ConnectivityObserverImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ConnectivityObserver {

    private val cm: ConnectivityManager by lazy {
        context.getSystemService<ConnectivityManager>()!!
    }

    override val status = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(NetworkStatus.Losing(maxMsToLive))
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost)
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) {
                val downKbps = nc.linkDownstreamBandwidthKbps
                val isValidated = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (downKbps in 0..1500 || !isValidated) {
                    trySend(NetworkStatus.Slow)
                }
            }
        }

        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(req, callback)
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
