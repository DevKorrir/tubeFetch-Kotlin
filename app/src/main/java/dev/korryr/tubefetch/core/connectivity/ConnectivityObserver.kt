package dev.korryr.tubefetch.core.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    val status: Flow<NetworkStatus>
}
