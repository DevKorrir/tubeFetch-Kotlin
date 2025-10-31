package dev.korryr.tubefetch.domain.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

data class DownloadItem @RequiresApi(Build.VERSION_CODES.O) constructor(
    val id: String,
    val title: String,
    val duration: String,
    val thumbnail: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val fileSize: String = "",
    val downloadSpeed: String = "",
    val quality: VideoQuality = VideoQuality.HD720,
    val format: DownloadFormat = DownloadFormat.MP4,
    val url: String = "",
    val channelName: String = "",
    val viewCount: String = "",
    val uploadDate: String = "",
    val downloadPath: String = "",
    val fileName: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val fileUri: String = "" // MediaStore URI
)