package dev.korryr.tubefetch.ui.features.history.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.korryr.tubefetch.data.remote.DownloadFilter
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.tracker.DownloadTracker
import dev.korryr.tubefetch.domain.usecase.ClearCompletedDownloadsUseCase
import dev.korryr.tubefetch.domain.usecase.DeleteDownloadUseCase
import dev.korryr.tubefetch.domain.usecase.GetDownloadHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsState(
    val downloads: List<DownloadItem> = emptyList(),
    val selectedFilter: DownloadFilter = DownloadFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class DownloadsEvent {
    data class FilterChanged(val filter: DownloadFilter) : DownloadsEvent()
    data class DeleteDownload(val downloadId: String) : DownloadsEvent()
    data class PauseDownload(val downloadId: String) : DownloadsEvent()
    data class ResumeDownload(val downloadId: String) : DownloadsEvent()
    data class RetryDownload(val downloadId: String) : DownloadsEvent()
    object ClearDownloads : DownloadsEvent()
    object ClearCompleted : DownloadsEvent()
    object DismissError : DownloadsEvent()
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val getDownloadHistoryUseCase: GetDownloadHistoryUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val clearCompletedDownloadsUseCase: ClearCompletedDownloadsUseCase,
    private val downloadTracker: DownloadTracker
) : ViewModel() {
    
    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    
    val state: StateFlow<DownloadsState> = combine(
        getDownloadHistoryUseCase(),
        _selectedFilter
    ) { downloads, filter ->
        DownloadsState(
            downloads = downloads,
            selectedFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsState(isLoading = true)
    )
    
    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.FilterChanged -> {
                _selectedFilter.value = event.filter
            }
            is DownloadsEvent.DeleteDownload -> {
                viewModelScope.launch {
                    deleteDownloadUseCase(event.downloadId)
                }
            }
            is DownloadsEvent.PauseDownload -> {
                viewModelScope.launch {
                    downloadTracker.pauseDownload(event.downloadId)
                }
            }
            is DownloadsEvent.ResumeDownload -> {
                viewModelScope.launch {
                    downloadTracker.resumeDownload(event.downloadId)
                }
            }
            is DownloadsEvent.RetryDownload -> {
                viewModelScope.launch {
                    downloadTracker.retryDownload(event.downloadId)
                }
            }
            DownloadsEvent.ClearDownloads -> {
                viewModelScope.launch {
                    // For now, just clear completed. You'll need a ClearAllDownloadsUseCase
                    clearCompletedDownloadsUseCase()
                }
            }
            DownloadsEvent.ClearCompleted -> {
                viewModelScope.launch {
                    clearCompletedDownloadsUseCase()
                }
            }
            DownloadsEvent.DismissError -> {
                // Handle error dismissal if needed
            }
        }
    }
    
    fun getFilteredDownloads(): List<DownloadItem> {
        val currentDownloads = state.value.downloads
        return when (_selectedFilter.value) {
            DownloadFilter.ALL -> currentDownloads
            DownloadFilter.DOWNLOADING -> currentDownloads.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
            }
            DownloadFilter.COMPLETED -> currentDownloads.filter { 
                it.status == DownloadStatus.COMPLETED 
            }
            DownloadFilter.FAILED -> currentDownloads.filter { 
                it.status == DownloadStatus.FAILED 
            }
            DownloadFilter.AUDIO -> currentDownloads.filter {
                it.format in listOf(DownloadFormat.MP3, DownloadFormat.M4A, DownloadFormat.WAV) // Adjust based on your DownloadFormat
            }
            DownloadFilter.VIDEO -> currentDownloads.filter {
                it.format in listOf(DownloadFormat.MP4, DownloadFormat.WEBM) // Adjust based on your DownloadFormat
            }
        }
    }
}