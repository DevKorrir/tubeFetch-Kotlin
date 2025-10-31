package dev.korryr.tubefetch.ui.features.home.viewModel

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.korryr.tubefetch.data.remote.DownloadFilter
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadRequest
import dev.korryr.tubefetch.domain.model.DownloadStats
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoInfo
import dev.korryr.tubefetch.domain.model.VideoQuality
import dev.korryr.tubefetch.domain.tracker.DownloadTracker
import dev.korryr.tubefetch.domain.usecase.AnalyzeVideoUseCase
import dev.korryr.tubefetch.domain.usecase.ClearCompletedDownloadsUseCase
import dev.korryr.tubefetch.domain.usecase.DeleteDownloadUseCase
import dev.korryr.tubefetch.domain.usecase.DownloadVideoUseCase
import dev.korryr.tubefetch.domain.usecase.GetDownloadHistoryUseCase
import dev.korryr.tubefetch.domain.usecase.UpdateDownloadUseCase
import dev.korryr.tubefetch.utils.PermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val analyzeVideoUseCase: AnalyzeVideoUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val getDownloadHistoryUseCase: GetDownloadHistoryUseCase,
    private val updateDownloadUseCase: UpdateDownloadUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val clearCompletedDownloadsUseCase: ClearCompletedDownloadsUseCase,
    private val permissionManager: PermissionManager,
    private val downloadTracker: DownloadTracker
) : ViewModel() {


    private val _state = mutableStateOf(HomeUiState())
    val state: State<HomeUiState> = _state

    private val _permissionState = mutableStateOf(PermissionState())
    val permissionState: State<PermissionState> = _permissionState

    // FIX: Use MutableSharedFlow instead of mutableSharedFlow
    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()


    init {
        loadDownloadHistory()
        observeDownloadProgress()
        checkPermissions()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.UrlChanged -> onUrlChanged(event.url)
            is HomeEvent.DownloadClicked -> onDownloadClicked()
            is HomeEvent.QualitySelected -> onQualitySelected(event.quality)
            is HomeEvent.FormatSelected -> onFormatSelected(event.format)
            is HomeEvent.PauseDownload -> onPauseDownload(event.downloadId)
            is HomeEvent.ResumeDownload -> onResumeDownload(event.downloadId)
            is HomeEvent.RetryDownload -> onRetryDownload(event.downloadId)
            is HomeEvent.DeleteDownload -> onDeleteDownload(event.downloadId)
            is HomeEvent.ClearDownloads -> onClearDownloads()
            is HomeEvent.ClearCompleted -> onClearCompleted()
            is HomeEvent.ShowQualitySheet -> showQualitySheet()
            is HomeEvent.HideQualitySheet -> hideQualitySheet()
            is HomeEvent.ShowFormatSheet -> showFormatSheet()
            is HomeEvent.HideFormatSheet -> hideFormatSheet()
            is HomeEvent.DismissError -> dismissError()
            is HomeEvent.RequestStoragePermission -> requestStoragePermission(event.activity)
            is HomeEvent.FilterChanged -> onFilterChanged(event.filter)
        }
    }

    private fun onUrlChanged(url: String) {
        _state.value = _state.value.copy(urlInput = url)

        if (isValidYouTubeUrl(url)) {
            analyzeVideo(url)
        } else {
            _state.value = _state.value.copy(videoInfo = null)
        }
    }

    private fun analyzeVideo(url: String) {
        _state.value = _state.value.copy(isAnalyzing = true)

        viewModelScope.launch {
            when (val result = analyzeVideoUseCase(url)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        videoInfo = result.data,
                        isAnalyzing = false
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isAnalyzing = false,
                        error = result.message
                    )
                    _sideEffects.emit(HomeSideEffect.ShowError(result.message))
                }
                is ApiResult.Loading -> {
                    // Handle loading state
                }
            }
        }
    }

    private fun onDownloadClicked() {
        val url = _state.value.urlInput
        if (url.isNotBlank()) {
            // Check permissions first
            if (!_permissionState.value.hasStoragePermission) {
                viewModelScope.launch {
                    _sideEffects.emit(HomeSideEffect.RequestStoragePermission)
                }
                return
            }

            viewModelScope.launch {
                val request = DownloadRequest(
                    url = url,
                    quality = _state.value.selectedQuality,
                    format = _state.value.selectedFormat,
                    title = _state.value.videoInfo?.title ?: "Downloaded Video",
                    fileName = generateFileName()
                )

                when (val result = downloadVideoUseCase(request)) {
                    is ApiResult.Success -> {
                        _state.value = _state.value.copy(
                            urlInput = "",
                            videoInfo = null
                        )
                        _sideEffects.emit(HomeSideEffect.ShowMessage("Download started!"))
                        loadDownloadHistory()
                    }
                    is ApiResult.Error -> {
                        _state.value = _state.value.copy(error = result.message)
                        _sideEffects.emit(HomeSideEffect.ShowError("Download failed: ${result.message}"))
                    }
                    is ApiResult.Loading -> {
                        // Handle loading
                    }
                }
            }
        }
    }

    private fun onPauseDownload(downloadId: String) {
        viewModelScope.launch {
            downloadTracker.pauseDownload(downloadId)
            _sideEffects.emit(HomeSideEffect.ShowMessage("Download paused"))
        }
    }

    private fun onQualitySelected(quality: VideoQuality) {
        _state.value = _state.value.copy(selectedQuality = quality)
        hideQualitySheet()
    }

    private fun onFormatSelected(format: DownloadFormat) {
        _state.value = _state.value.copy(selectedFormat = format)
        hideFormatSheet()
    }

    private fun onResumeDownload(downloadId: String) {
        viewModelScope.launch {
            downloadTracker.resumeDownload(downloadId)
            _sideEffects.emit(HomeSideEffect.ShowMessage("Download resumed"))
        }
    }

    private fun onRetryDownload(downloadId: String) {
        viewModelScope.launch {
            downloadTracker.retryDownload(downloadId)
            _sideEffects.emit(HomeSideEffect.ShowMessage("Retrying download..."))
        }
    }

    private fun onDeleteDownload(downloadId: String) {
        viewModelScope.launch {
            deleteDownloadUseCase(downloadId)
            loadDownloadHistory()
            _sideEffects.emit(HomeSideEffect.ShowMessage("Download deleted"))
        }
    }

    private fun onClearDownloads() {
        viewModelScope.launch {
            // Implementation for clearing all downloads
            loadDownloadHistory()
            _sideEffects.emit(HomeSideEffect.ShowMessage("All downloads cleared"))
        }
    }

    private fun onClearCompleted() {
        viewModelScope.launch {
            clearCompletedDownloadsUseCase()
            loadDownloadHistory()
            _sideEffects.emit(HomeSideEffect.ShowMessage("Completed downloads cleared"))
        }
    }

    private fun onFilterChanged(filter: DownloadFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    private fun showQualitySheet() {
        _state.value = _state.value.copy(showQualitySheet = true)
    }

    private fun hideQualitySheet() {
        _state.value = _state.value.copy(showQualitySheet = false)
    }

    private fun showFormatSheet() {
        _state.value = _state.value.copy(showFormatSheet = true)
    }

    private fun hideFormatSheet() {
        _state.value = _state.value.copy(showFormatSheet = false)
    }

    private fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadDownloadHistory() {
        viewModelScope.launch {
            getDownloadHistoryUseCase()
                .collect { downloads ->
                    _state.value = _state.value.copy(
                        downloads = downloads,
                        isLoading = false
                    )
                    updateDownloadStats(downloads)
                }
        }
    }

    private fun updateDownloadStats(downloads: List<DownloadItem>) {
        val totalSize = downloads.sumOf {
            it.fileSize.replace(" MB", "").toDoubleOrNull() ?: 0.0
        }

        _state.value = _state.value.copy(
            downloadStats = DownloadStats(
                totalDownloads = downloads.size,
                completedDownloads = downloads.count { it.status == DownloadStatus.COMPLETED },
                totalSize = "${String.format("%.1f", totalSize)} MB",
                activeDownloads = downloads.count {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                }
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeDownloadProgress() {
        viewModelScope.launch {
            downloadTracker.downloadProgress.collect { progressUpdate ->

                // Handle null case
                progressUpdate?.let { update ->

                    val currentDownloads = _state.value.downloads.toMutableList()
                    val index = currentDownloads.indexOfFirst { it.id == update.downloadId }
                    if (index != -1) {
                        currentDownloads[index] = currentDownloads[index].copy(
                            progress = update.progress,
                            downloadSpeed = update.speed,
                            fileSize = update.fileSize,
                            status = update.status
                        )
                        _state.value = _state.value.copy(downloads = currentDownloads)
                        updateDownloadStats(currentDownloads)
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val hasPermission = permissionManager.hasStoragePermission(
            )
            _permissionState.value = _permissionState.value.copy(hasStoragePermission = hasPermission)
        }
    }

    fun requestStoragePermission(activity: Activity) {
        permissionManager.requestStoragePermission(activity, REQUEST_STORAGE_PERMISSION)
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionState.value = _permissionState.value.copy(hasStoragePermission = granted)
        if (granted) {
            viewModelScope.launch {
                _sideEffects.emit(HomeSideEffect.ShowMessage("Storage permission granted"))
            }
        } else {
            viewModelScope.launch {
                _sideEffects.emit(HomeSideEffect.ShowError("Storage permission required for downloads"))
            }
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

    companion object {
        const val REQUEST_STORAGE_PERMISSION = 1001
    }
}

// Enhanced State Classes
data class HomeUiState(
    val urlInput: String = "",
    val selectedQuality: VideoQuality = VideoQuality.AUTO,
    val selectedFormat: DownloadFormat = DownloadFormat.MP4,
    val videoInfo: VideoInfo? = null,
    val downloads: List<DownloadItem> = emptyList(),
    val downloadStats: DownloadStats = DownloadStats(0, 0, "0 MB", 0),
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val showQualitySheet: Boolean = false,
    val showFormatSheet: Boolean = false,
    val selectedFilter: DownloadFilter = DownloadFilter.ALL
)

data class PermissionState(
    val hasStoragePermission: Boolean = false
)

sealed class HomeEvent {
    data class UrlChanged(val url: String) : HomeEvent()
    data object DownloadClicked : HomeEvent()
    data class QualitySelected(val quality: VideoQuality) : HomeEvent()
    data class FormatSelected(val format: DownloadFormat) : HomeEvent()
    data class PauseDownload(val downloadId: String) : HomeEvent()
    data class ResumeDownload(val downloadId: String) : HomeEvent()
    data class RetryDownload(val downloadId: String) : HomeEvent()
    data class DeleteDownload(val downloadId: String) : HomeEvent()
    data object ClearDownloads : HomeEvent()
    data object ClearCompleted : HomeEvent()
    data object ShowQualitySheet : HomeEvent()
    data object HideQualitySheet : HomeEvent()
    data object ShowFormatSheet : HomeEvent()
    data object HideFormatSheet : HomeEvent()
    data object DismissError : HomeEvent()
    data class RequestStoragePermission(val activity: Activity) : HomeEvent()
    data class FilterChanged(val filter: DownloadFilter) : HomeEvent()
}

sealed class HomeSideEffect {
    data class ShowMessage(val message: String) : HomeSideEffect()
    data class ShowError(val message: String) : HomeSideEffect()
    data object RequestStoragePermission : HomeSideEffect()
}