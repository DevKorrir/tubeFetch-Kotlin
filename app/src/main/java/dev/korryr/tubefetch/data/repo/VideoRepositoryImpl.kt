package dev.korryr.tubefetch.data.repo

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import dev.korryr.tubefetch.data.local.dao.DownloadDao
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.remote.YouTubeWebServiceImpl
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadRequest
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoInfo
import dev.korryr.tubefetch.domain.model.VideoQuality
import dev.korryr.tubefetch.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val youTubeWebService: YouTubeWebServiceImpl, // ADD THIS
    private val downloadDao: DownloadDao,
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) : VideoRepository {

    override suspend fun analyzeVideo(url: String): ApiResult<VideoInfo> {
        return try {
            // Use the Web API service instead
            youTubeWebService.getVideoInfo(url)
        } catch (e: Exception) {
            ApiResult.Error("Network error: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun downloadVideo(request: DownloadRequest): ApiResult<Unit> {
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

            // For Web API, we need to handle download differently
            // Since Web API returns a download URL, we need to download the file ourselves
            val downloadResult = downloadWithWebApi(request, downloadDir)

            when (downloadResult) {
                is ApiResult.Success -> {
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
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> {
                    // Update with error
                    val failedItem = downloadItem.copy(
                        status = DownloadStatus.FAILED
                    )
                    downloadDao.updateDownload(failedItem.toEntity())
                    ApiResult.Error("Download failed: ${downloadResult.message}", downloadResult.exception)
                }
                is ApiResult.Loading -> {
                    ApiResult.Error("Download still in progress")
                }
            }
        } catch (e: Exception) {
            ApiResult.Error("Download error: ${e.message}", e)
        }
    }


    private suspend fun downloadWithWebApi(
        request: DownloadRequest,
        outputDir: File
    ): ApiResult<File> {
        return try {
            // Step 1: Get download URL from Web API
            val downloadUrlResult = youTubeWebService.getDownloadUrl(
                url = request.url,
                format = request.format.extension
            )

            when (downloadUrlResult) {
                is ApiResult.Success -> {
                    // Step 2: Download the file from the URL
                    val downloadedFile = downloadFileFromUrl(
                        downloadUrl = downloadUrlResult.data.url,
                        outputDir = outputDir,
                        fileName = request.fileName
                    )

                    if (downloadedFile != null && downloadedFile.exists()) {
                        ApiResult.Success(downloadedFile)
                    } else {
                        ApiResult.Error("Failed to download file from URL")
                    }
                }
                is ApiResult.Error -> {
                    ApiResult.Error("Failed to get download URL: ${downloadUrlResult.message}")
                }
                is ApiResult.Loading -> {
                    ApiResult.Error("Still getting download URL")
                }
            }
        } catch (e: Exception) {
            ApiResult.Error("Web API download failed: ${e.message}")
        }
    }


    private suspend fun downloadFileFromUrl(
        downloadUrl: String,
        outputDir: File,
        fileName: String
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val outputFile = File(outputDir, fileName)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    response.body?.byteStream()?.use { inputStream ->
                        outputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                outputFile
            } catch (e: Exception) {
                null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getDownloadHistory(): Flow<List<DownloadItem>> {
        return downloadDao.getDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getDownloadById(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toDownloadItem()
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
            .first()
            .filter { it.status == "COMPLETED" }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun DownloadItem.toEntity(): dev.korryr.tubefetch.data.local.entity.DownloadEntity {
        return dev.korryr.tubefetch.data.local.entity.DownloadEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnail = thumbnail,
            status = status.name,
            progress = progress,
            fileSize = fileSize,
            downloadSpeed = downloadSpeed,
            quality = quality.name,
            format = format.name,
            url = url,
            channelName = channelName,
            viewCount = viewCount,
            uploadDate = uploadDate,
            downloadPath = downloadPath,
            fileUri = fileUri,
            createdAt = createdAt
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dev.korryr.tubefetch.data.local.entity.DownloadEntity.toDownloadItem(): DownloadItem {
        return DownloadItem(
            id = id,
            title = title,
            duration = duration,
            thumbnail = thumbnail,
            status = DownloadStatus.valueOf(status),
            progress = progress,
            fileSize = fileSize,
            downloadSpeed = downloadSpeed,
            quality = VideoQuality.values().find { it.name == quality } ?: VideoQuality.AUTO,
            format = DownloadFormat.values().find { it.name == format } ?: DownloadFormat.MP4,
            url = url,
            channelName = channelName,
            viewCount = viewCount,
            uploadDate = uploadDate,
            downloadPath = downloadPath,
            fileUri = fileUri,
            createdAt = createdAt
        )
    }
}