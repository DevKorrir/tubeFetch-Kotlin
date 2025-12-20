package dev.korryr.tubefetch.core.connectivity

sealed class NetworkStatus {
    data object Available : NetworkStatus()
    data class Losing(val maxMsToLive: Int?) : NetworkStatus()
    data object Lost : NetworkStatus()
    data object Unavailable : NetworkStatus()
    data object Slow : NetworkStatus()
}
