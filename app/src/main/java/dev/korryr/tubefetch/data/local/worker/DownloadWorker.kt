package dev.korryr.tubefetch.data.local.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.repo.VideoRepositoryImpl
import dev.korryr.tubefetch.domain.model.DownloadStatus
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoRepositoryImpl,
    private val fileStorageManager: FileStorageManager,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        
        return try {
            // Get download info from database
            val download = repository.getDownloadById(downloadId)
            if (download == null) {
                return Result.failure()
            }
            
            // Update status to downloading
            repository.updateDownload(download.copy(status = DownloadStatus.DOWNLOADING))
            
            // Simulate download process (replace with actual download logic)
            for (progress in 0..100 step 10) {
                if (isStopped) {
                    repository.updateDownload(download.copy(status = DownloadStatus.PAUSED))
                    return Result.success()
                }
                
                repository.updateDownload(
                    download.copy(
                        progress = progress / 100f,
                        downloadSpeed = "${(100 + progress)} KB/s"
                    )
                )
                delay(500)
            }
            
            // Mark as completed
            repository.updateDownload(
                download.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    fileSize = "45.2 MB"
                )
            )
            
            Result.success()
        } catch (e: Exception) {
            // Update status to failed
            val download = repository.getDownloadById(downloadId)
            download?.let {
                repository.updateDownload(it.copy(status = DownloadStatus.FAILED))
            }
            Result.failure()
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        
        fun createInputData(downloadId: String): Data {
            return Data.Builder()
                .putString(KEY_DOWNLOAD_ID, downloadId)
                .build()
        }
    }
}