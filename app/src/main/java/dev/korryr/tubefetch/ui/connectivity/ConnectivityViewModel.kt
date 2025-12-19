package dev.korryr.tubefetch.ui.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.korryr.tubefetch.core.connectivity.ConnectivityObserver
import dev.korryr.tubefetch.core.connectivity.NetworkStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    observer: ConnectivityObserver
) : ViewModel() {
    val status: StateFlow<NetworkStatus> =
        observer.status.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NetworkStatus.Unavailable
        )
}
