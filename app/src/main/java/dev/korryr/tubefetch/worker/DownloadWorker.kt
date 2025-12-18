package dev.korryr.tubefetch.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.korryr.tubefetch.R
import dev.korryr.tubefetch.data.local.dao.DownloadDao
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.local.mappers.toDownloadItem
import dev.korryr.tubefetch.data.local.mappers.toEntity
import dev.korryr.tubefetch.data.remote.YouTubeWebServiceImpl
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val youTubeWebService: YouTubeWebServiceImpl,
    private val fileStorageManager: FileStorageManager
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return Result.failure()
        val qualityName = inputData.getString(KEY_QUALITY) ?: return Result.failure()
        val formatName = inputData.getString(KEY_FORMAT) ?: return Result.failure()

        val quality = try {
            VideoQuality.valueOf(qualityName)
        } catch (e: IllegalArgumentException) {
            VideoQuality.AUTO
        }
        val format = try {
            DownloadFormat.valueOf(formatName)
        } catch (e: IllegalArgumentException) {
            DownloadFormat.MP4
        }

        val notificationId = id.hashCode()
        val notification = createNotification(title)
        setForeground(ForegroundInfo(notificationId, notification))

        return try {
            // Update status to DOWNLOADING
            updateDownloadStatus(title, DownloadStatus.DOWNLOADING, 0f)
            
            // Get download URL from API
            val downloadUrlResult = youTubeWebService.getDownloadUrl(url, format.extension, quality)
            
            when (downloadUrlResult) {
                is ApiResult.Success -> {
                    // Download the file with progress
                    val downloadResult = downloadFileWithProgress(
                        downloadUrlResult.data.url,
                        fileName,
                        format,
                        title,
                        notificationId
                    )
                    
                    when (downloadResult) {
                        is ApiResult.Success -> {
                            // Update status to COMPLETED
                            updateDownloadStatus(title, DownloadStatus.COMPLETED, 1.0f)
                            showCompletionNotification(title)
                            Result.success()
                        }
                        is ApiResult.Error -> {
                            updateDownloadStatus(title, DownloadStatus.FAILED, 0f)
                            Result.failure()
                        }
                        else -> Result.failure()
                    }
                }
                is ApiResult.Error -> {
                    updateDownloadStatus(title, DownloadStatus.FAILED, 0f)
                    Result.failure()
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            updateDownloadStatus(title, DownloadStatus.FAILED, 0f)
            Result.failure()
        }
    }

    private suspend fun downloadFileWithProgress(
        url: String,
        fileName: String,
        format: DownloadFormat,
        title: String,
        notificationId: Int
    ): ApiResult<File> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                // Create download directory
                val downloadDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "TubeFetch"
                ).apply { mkdirs() }
                
                val outputFile = File(downloadDir, fileName)
                
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext ApiResult.Error("HTTP ${response.code}")
                    }
                    
                    val contentLength = response.body?.contentLength() ?: -1
                    val inputStream = response.body?.byteStream()
                        ?: return@withContext ApiResult.Error("No response body")
                    
                    inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesCopied = 0L
                            var bytesRead: Int
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesCopied += bytesRead
                                
                                // Update progress
                                if (contentLength > 0) {
                                    val progress = bytesCopied.toFloat() / contentLength.toFloat()
                                    updateDownloadStatus(title, DownloadStatus.DOWNLOADING, progress)
                                    updateProgressNotification(title, progress, notificationId)
                                }
                            }
                        }
                    }
                    
                    // Save to MediaStore
                    val fileUri = fileStorageManager.saveFileToMediaStore(
                        downloadedFile = outputFile,
                        fileName = fileName,
                        format = format
                    )
                    
                    // Update download item with file info
                    val downloadItem = downloadDao.getDownloads()
                        .flowOn(Dispatchers.IO)
                        .first()
                        .find { it.title == title }
                        ?.toDownloadItem()
                    
                    downloadItem?.let {
                        val updatedItem = it.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1.0f,
                            fileSize = formatFileSize(outputFile.length()),
                            downloadPath = outputFile.absolutePath,
                            fileUri = fileUri?.toString() ?: ""
                        )
                        downloadDao.updateDownload(updatedItem.toEntity())
                    }
                    
                    ApiResult.Success(outputFile)
                }
            } catch (e: IOException) {
                ApiResult.Error("Download failed: ${e.message}")
            } catch (e: Exception) {
                ApiResult.Error("Unexpected error: ${e.message}")
            }
        }
    }

    private suspend fun updateDownloadStatus(title: String, status: DownloadStatus, progress: Float) {
        try {
            val downloadItem = downloadDao.getDownloads()
                .flowOn(Dispatchers.IO)
                .first()
                .find { it.title == title }
                ?.toDownloadItem()
            
            downloadItem?.let {
                val updatedItem = it.copy(
                    status = status,
                    progress = progress
                )
                downloadDao.updateDownload(updatedItem.toEntity())
            }
        } catch (e: Exception) {
            // Ignore errors in status updates
        }
    }

    private fun updateProgressNotification(title: String, progress: Float, notificationId: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading: ${(progress * 100).toInt()}%")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, (progress * 100).toInt(), false)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }

    private fun showCompletionNotification(title: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Download completed")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(id.hashCode(), notification)
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    private fun createNotification(title: String) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("Download in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper download icon
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        const val KEY_URL = "KEY_URL"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_FILE_NAME = "KEY_FILE_NAME"
        const val KEY_QUALITY = "KEY_QUALITY"
        const val KEY_FORMAT = "KEY_FORMAT"

        const val CHANNEL_ID = "TubeFetch-Downloads"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Downloads"
                val descriptionText = "Shows active and completed downloads"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
