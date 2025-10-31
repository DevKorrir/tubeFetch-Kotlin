package dev.korryr.tubefetch.domain.tracker

import android.R.attr.delay
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DownloadTracker @Inject constructor(
    private val repository: VideoRepository
) {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val activeDownloads = mutableMapOf<String, Job>()

    fun pauseDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
    }

    fun resumeDownload(downloadId: String) {
        // Implementation for resuming download
        updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
    }

    fun retryDownload(downloadId: String) {
        // Implementation for retrying failed download
        updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
    }

    private fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        // Update in repository
        // This would be connected to your actual download service
    }

    // Simulate download progress for demo
    fun simulateDownloadProgress(downloadId: String) {
        activeDownloads[downloadId] = CoroutineScope(Dispatchers.IO).launch {
            var progress = 0f
            while (progress < 1f) {
                delay(500) // Simulate download delay
                progress += 0.1f
                
                _downloadProgress.value = DownloadProgress(
                    downloadId = downloadId,
                    progress = progress,
                    speed = "${(100 + progress * 100).toInt()} KB/s",
                    fileSize = "${(progress * 450).toInt()} MB",
                    status = if (progress < 1f) DownloadStatus.DOWNLOADING else DownloadStatus.COMPLETED
                )
                
                if (progress >= 1f) {
                    break
                }
            }
        }
    }
}

data class DownloadProgress(
    val downloadId: String,
    val progress: Float,
    val speed: String,
    val fileSize: String,
    val status: DownloadStatus
)