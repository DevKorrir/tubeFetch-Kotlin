package dev.korryr.tubefetch.data.local.entity

import androidx.room.*
import java.time.LocalDateTime

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val duration: String,
    val thumbnail: String,
    val status: String,
    val progress: Float = 0f,
    val fileSize: String = "",
    val downloadSpeed: String = "",
    val quality: String = "",
    val format: String = "",
    val url: String = "",
    val channelName: String = "",
    val viewCount: String = "",
    val uploadDate: String = "",
    val downloadPath: String = "",
    val fileUri: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)