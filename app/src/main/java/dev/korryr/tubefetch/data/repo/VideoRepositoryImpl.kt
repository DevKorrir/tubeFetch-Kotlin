package dev.korryr.tubefetch.data.repo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dev.korryr.tubefetch.data.local.dao.DownloadDao
import dev.korryr.tubefetch.data.remote.VideoService
import dev.korryr.tubefetch.domain.model.*
import dev.korryr.tubefetch.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val videoService: VideoService,
    private val downloadDao: DownloadDao,
    private val context: Context
) : VideoRepository {

    override suspend fun analyzeVideo(url: String): Result<VideoInfo> {
        return try {
            val response = videoService.getVideoInfo(url)
            val videoInfo = response.toVideoInfo()
            Result.Success(videoInfo)
        } catch (e: Exception) {
            Result.Error("Failed to analyze video: ${e.message}", e)
        }
    }

    override suspend fun downloadVideo(request: DownloadRequest): Result<Unit> {
        return try {
            // Get download URL from service
            val downloadResponse = videoService.getDownloadUrl(
                url = request.url,
                format = request.format.name,
                quality = request.quality.name
            )
            
            // Create download item in database
            val downloadItem = DownloadItem(
                id = UUID.randomUUID().toString(),
                title = request.title,
                duration = "",
                thumbnail = "",
                status = DownloadStatus.QUEUED,
                quality = request.quality,
                format = request.format,
                url = request.url,
                fileName = request.fileName
            )
            
            // Start download process (we'll implement this with WorkManager)
            // For now, just save to database
            downloadDao.insertDownload(downloadItem.toEntity())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Download failed: ${e.message}", e)
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
        downloadDao.clearCompletedDownloads()
    }

    private fun VideoInfoResponse.toVideoInfo(): VideoInfo {
        return VideoInfo(
            title = title,
            duration = duration,
            thumbnail = thumbnail,
            channelName = channel.name,
            viewCount = viewCount,
            uploadDate = uploadDate,
            description = description,
            availableQualities = formats.mapNotNull { format ->
                VideoQuality.values().find { it.displayName == format.qualityLabel }
            }
        )
    }

    private fun DownloadEntity.toDownloadItem(): DownloadItem {
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

    private fun DownloadItem.toEntity(): DownloadEntity {
        return DownloadEntity(
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

    private suspend fun deleteFileFromStorage(fileUri: String) {
        try {
            val uri = Uri.parse(fileUri)
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Log error but don't fail
            e.printStackTrace()
        }
    }
}