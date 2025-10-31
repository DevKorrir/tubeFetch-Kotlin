package dev.korryr.tubefetch.data.repo

import android.content.Context
import android.net.Uri
import android.os.Environment
import dev.korryr.tubefetch.data.local.dao.DownloadDao
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.remote.YouTubeNativeService
import dev.korryr.tubefetch.domain.model.*
import dev.korryr.tubefetch.domain.repository.VideoRepository
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val youTubeService: YouTubeNativeService,
    private val downloadDao: DownloadDao,
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) : VideoRepository {

    override suspend fun analyzeVideo(url: String): Result<VideoInfo> {
        return try {
            val result = youTubeService.getVideoInfo(url)
            when (result) {
                is Result.Success -> Result.Success(result.data)
                is Result.Error -> Result.Error("Failed to analyze video: ${result.exception?.message}", result.exception)
                else -> Result.Error("Unknown error analyzing video")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}", e)
        }
    }

    override suspend fun downloadVideo(request: DownloadRequest): Result<Unit> {
        return try {
            // Create download directory
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TubeFetch"
            ).apply { mkdirs() }

            // Create download item
            val downloadItem = DownloadItem(
                id = UUID.randomUUID().toString(),
                title = request.title,
                duration = "",
                thumbnail = "",
                status = DownloadStatus.DOWNLOADING,
                quality = request.quality,
                format = request.format,
                url = request.url,
                fileName = request.fileName
            )

            // Save to database
            downloadDao.insertDownload(downloadItem.toEntity())

            // Start download
            val downloadResult = youTubeService.downloadVideo(
                url = request.url,
                outputDir = downloadDir,
                format = request.format.extension,
                quality = request.quality.name
            )

            when (downloadResult) {
                is Result.Success -> {
                    // Move file to MediaStore and get URI
                    val fileUri = fileStorageManager.saveFileToMediaStore(
                        downloadedFile = downloadResult.data,
                        fileName = request.fileName,
                        format = request.format
                    )

                    // Update download item with completion
                    val completedItem = downloadItem.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1.0f,
                        fileSize = formatFileSize(downloadResult.data.length()),
                        downloadPath = downloadResult.data.absolutePath,
                        fileUri = fileUri?.toString() ?: ""
                    )

                    downloadDao.updateDownload(completedItem.toEntity())
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    // Update with error
                    val failedItem = downloadItem.copy(
                        status = DownloadStatus.FAILED
                    )
                    downloadDao.updateDownload(failedItem.toEntity())
                    Result.Error("Download failed: ${downloadResult.exception?.message}", downloadResult.exception)
                }
            }
        } catch (e: Exception) {
            Result.Error("Download error: ${e.message}", e)
        }
    }

    override suspend fun getDownloadHistory(): List<DownloadItem> {
        return downloadDao.getDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }

    override suspend fun getDownloadById(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toDownloadItem()
    }

    override suspend fun updateDownload(download: DownloadItem) {
        downloadDao.updateDownload(download.toEntity())
    }

    override suspend fun deleteDownload(id: String) {
        val entity = downloadDao.getDownloadById(id)
        entity?.let {
            // Delete actual file
            deleteFileFromStorage(it.fileUri)
            downloadDao.deleteDownload(it)
        }
    }

    override suspend fun clearCompletedDownloads() {
        // Get completed downloads first to delete their files
        val completedDownloads = downloadDao.getDownloads()
            .first { entities ->
                entities.filter { it.status == "COMPLETED" }
            }

        completedDownloads.forEach { entity ->
            deleteFileFromStorage(entity.fileUri)
        }

        downloadDao.clearCompletedDownloads()
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    private suspend fun deleteFileFromStorage(fileUri: String) {
        if (fileUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(fileUri)
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                // If MediaStore deletion fails, try direct file deletion
                try {
                    File(fileUri).delete()
                } catch (fileE: Exception) {
                    // Log but don't fail
                    fileE.printStackTrace()
                }
            }
        }
    }
}