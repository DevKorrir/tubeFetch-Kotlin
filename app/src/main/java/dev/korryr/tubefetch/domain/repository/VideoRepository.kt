package dev.korryr.tubefetch.domain.repository

import dev.korryr.tubefetch.domain.model.*
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun analyzeVideo(url: String): ApiResult<VideoInfo>
    suspend fun downloadVideo(request: DownloadRequest): ApiResult<Unit>
    fun getDownloadHistory(): Flow<List<DownloadItem>>
    suspend fun getDownloadById(id: String): DownloadItem?
    suspend fun updateDownload(download: DownloadItem)
    suspend fun deleteDownload(id: String)
    suspend fun clearCompletedDownloads()
}