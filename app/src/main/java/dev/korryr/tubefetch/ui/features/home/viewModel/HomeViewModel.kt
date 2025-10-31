package dev.korryr.tubefetch.ui.features.home.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.korryr.tubefetch.domain.model.*
import dev.korryr.tubefetch.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val analyzeVideoUseCase: AnalyzeVideoUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val getDownloadHistoryUseCase: GetDownloadHistoryUseCase,
    private val updateDownloadUseCase: UpdateDownloadUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val clearCompletedDownloadsUseCase: ClearCompletedDownloadsUseCase,
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _sideEffects = MutableStateFlow<HomeSideEffect?>(null)
    val sideEffects: StateFlow<HomeSideEffect?> = _sideEffects.asStateFlow()

    init {
        loadDownloadHistory()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UrlChanged -> onUrlChanged(event.url)
            is HomeEvent.DownloadClicked -> onDownloadClicked()
            is HomeEvent.QualitySelected -> onQualitySelected(event.quality)
            is HomeEvent.FormatSelected -> onFormatSelected(event.format)
            is HomeEvent.PauseDownload -> onPauseDownload(event.downloadId)
            is HomeEvent.RetryDownload -> onRetryDownload(event.downloadId)
            is HomeEvent.DeleteDownload -> onDeleteDownload(event.downloadId)
            is HomeEvent.ClearDownloads -> onClearDownloads()
            is HomeEvent.ClearCompleted -> onClearCompleted()
            is HomeEvent.ShowQualitySheet -> showQualitySheet()
            is HomeEvent.HideQualitySheet -> hideQualitySheet()
            is HomeEvent.ShowFormatSheet -> showFormatSheet()
            is HomeEvent.HideFormatSheet -> hideFormatSheet()
            is HomeEvent.DismissError -> dismissError()
        }
    }

    private fun onUrlChanged(url: String) {
        _state.update { it.copy(urlInput = url) }
        
        if (isValidYouTubeUrl(url)) {
            analyzeVideo(url)
        } else {
            _state.update { it.copy(videoInfo = null) }
        }
    }

    private fun analyzeVideo(url: String) {
        _state.update { it.copy(isAnalyzing = true) }
        
        viewModelScope.launch {
            when (val result = analyzeVideoUseCase(url)) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            videoInfo = result.data,
                            isAnalyzing = false
                        ) 
                    }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isAnalyzing = false,
                            error = result.message
                        ) 
                    }
                }
                is Result.Loading -> {
                    // Handle loading if needed
                }
            }
        }
    }

    private fun onDownloadClicked() {
        val url = _state.value.urlInput
        if (url.isNotBlank()) {
            viewModelScope.launch {
                val request = DownloadRequest(
                    url = url,
                    quality = _state.value.selectedQuality,
                    format = _state.value.selectedFormat,
                    title = _state.value.videoInfo?.title ?: "Downloaded Video",
                    fileName = generateFileName()
                )
                
                when (val result = downloadVideoUseCase(request)) {
                    is Result.Success -> {
                        _state.update { 
                            it.copy(
                                urlInput = "",
                                videoInfo = null
                            ) 
                        }
                        _sideEffects.value = HomeSideEffect.ShowMessage("Download started!")
                        loadDownloadHistory()
                    }
                    is Result.Error -> {
                        _state.update { it.copy(error = result.message) }
                    }
                    is Result.Loading -> {
                        // Handle loading
                    }
                }
            }
        }
    }

    private fun onQualitySelected(quality: VideoQuality) {
        _state.update { it.copy(selectedQuality = quality) }
        hideQualitySheet()
    }

    private fun onFormatSelected(format: DownloadFormat) {
        _state.update { it.copy(selectedFormat = format) }
        hideFormatSheet()
    }

    private fun onPauseDownload(downloadId: String) {
        viewModelScope.launch {
            // Implementation for pausing downloads
        }
    }

    private fun onRetryDownload(downloadId: String) {
        viewModelScope.launch {
            // Implementation for retrying downloads
        }
    }

    private fun onDeleteDownload(downloadId: String) {
        viewModelScope.launch {
            deleteDownloadUseCase(downloadId)
            loadDownloadHistory()
            _sideEffects.value = HomeSideEffect.ShowMessage("Download deleted")
        }
    }

    private fun onClearDownloads() {
        viewModelScope.launch {
            // Implementation for clearing all downloads
            loadDownloadHistory()
            _sideEffects.value = HomeSideEffect.ShowMessage("All downloads cleared")
        }
    }

    private fun onClearCompleted() {
        viewModelScope.launch {
            clearCompletedDownloadsUseCase()
            loadDownloadHistory()
            _sideEffects.value = HomeSideEffect.ShowMessage("Completed downloads cleared")
        }
    }

    private fun showQualitySheet() {
        _state.update { it.copy(showQualitySheet = true) }
    }

    private fun hideQualitySheet() {
        _state.update { it.copy(showQualitySheet = false) }
    }

    private fun showFormatSheet() {
        _state.update { it.copy(showFormatSheet = true) }
    }

    private fun hideFormatSheet() {
        _state.update { it.copy(showFormatSheet = false) }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun loadDownloadHistory() {
        viewModelScope.launch {
            val downloads = getDownloadHistoryUseCase()
            _state.update { it.copy(downloads = downloads) }
        }
    }

    private fun generateFileName(): String {
        val title = _state.value.videoInfo?.title ?: "video"
        val cleanTitle = title.replace("[^a-zA-Z0-9\\-_]".toRegex(), "_")
        val extension = _state.value.selectedFormat.extension
        return "$cleanTitle.$extension"
    }

    private fun isValidYouTubeUrl(url: String): Boolean {
        val patterns = listOf(
            Regex("^https?://(www\\.)?youtube\\.com/watch\\?v=[\\w-]+"),
            Regex("^https?://(www\\.)?youtu\\.be/[\\w-]+"),
            Regex("^https?://(www\\.)?youtube\\.com/embed/[\\w-]+")
        )
        return patterns.any { it.matches(url) }
    }
}

data class HomeState(
    val urlInput: String = "",
    val selectedQuality: VideoQuality = VideoQuality.AUTO,
    val selectedFormat: DownloadFormat = DownloadFormat.MP4,
    val videoInfo: VideoInfo? = null,
    val downloads: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val showQualitySheet: Boolean = false,
    val showFormatSheet: Boolean = false
)

sealed class HomeEvent {
    data object DownloadClicked : HomeEvent()
    data class UrlChanged(val url: String) : HomeEvent()
    data class QualitySelected(val quality: VideoQuality) : HomeEvent()
    data class FormatSelected(val format: DownloadFormat) : HomeEvent()
    data class PauseDownload(val downloadId: String) : HomeEvent()
    data class RetryDownload(val downloadId: String) : HomeEvent()
    data class DeleteDownload(val downloadId: String) : HomeEvent()
    data object ClearDownloads : HomeEvent()
    data object ClearCompleted : HomeEvent()
    data object ShowQualitySheet : HomeEvent()
    data object HideQualitySheet : HomeEvent()
    data object ShowFormatSheet : HomeEvent()
    data object HideFormatSheet : HomeEvent()
    data object DismissError : HomeEvent()
}

sealed class HomeSideEffect {
    data class ShowMessage(val message: String) : HomeSideEffect()
    data class ShowError(val message: String) : HomeSideEffect()
}