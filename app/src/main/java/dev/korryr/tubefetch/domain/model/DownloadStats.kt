package dev.korryr.tubefetch.domain.model

data class DownloadStats(
    val totalDownloads: Int,
    val completedDownloads: Int,
    val totalSize: String,
    val activeDownloads: Int
)