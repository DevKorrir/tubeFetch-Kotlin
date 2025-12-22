package dev.korryr.tubefetch.data.repo

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.korryr.tubefetch.data.local.dao.DownloadDao
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.local.mappers.toDownloadItem
import dev.korryr.tubefetch.data.local.mappers.toEntity
import dev.korryr.tubefetch.data.remote.YouTubeWebServiceImpl
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadRequest
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoInfo
import dev.korryr.tubefetch.domain.model.VideoQuality
import dev.korryr.tubefetch.domain.repository.VideoRepository
import dev.korryr.tubefetch.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val youTubeWebService: YouTubeWebServiceImpl,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context,
    private val fileStorageManager: FileStorageManager,
    private val workManager: WorkManager
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
            // Create download item with QUEUED status
            val downloadItem = DownloadItem(
                id = UUID.randomUUID().toString(),
                title = request.title,
                duration = "",
                thumbnail = "",
                status = DownloadStatus.QUEUED,
                quality = request.quality,
                format = request.format,
                url = request.url,
                fileName = request.fileName,
                createdAt = LocalDateTime.now()
            )

            // Save to database first
            downloadDao.insertDownload(downloadItem.toEntity())

            // Create WorkManager request
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(DownloadWorker.KEY_URL, request.url)
                .putString(DownloadWorker.KEY_TITLE, request.title)
                .putString(DownloadWorker.KEY_FILE_NAME, request.fileName)
                .putString(DownloadWorker.KEY_QUALITY, request.quality.name)
                .putString(DownloadWorker.KEY_FORMAT, request.format.name)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            // Enqueue the work
            workManager.enqueue(workRequest)

            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error("Failed to start download: ${e.message}", e)
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
        val download = downloadDao.getDownloadById(id)
        download?.let {
            // Delete from MediaStore if file exists
            if (it.fileUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(it.fileUri)
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    // If MediaStore deletion fails, try direct file deletion
                    try {
                        File(it.fileUri).delete()
                    } catch (fileE: Exception) {
                        // Log but don't fail
                        fileE.printStackTrace()
                    }
                }
            }
            downloadDao.deleteDownload(it)
        }
    }

    override suspend fun clearCompletedDownloads() {
        val completedDownloads = downloadDao.getDownloads()
            .first()
            .filter { it.status == DownloadStatus.COMPLETED.name }
        
        completedDownloads.forEach { download ->
            // Delete files from MediaStore
            if (download.fileUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(download.fileUri)
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            downloadDao.deleteDownload(download)
        }
    }
}
