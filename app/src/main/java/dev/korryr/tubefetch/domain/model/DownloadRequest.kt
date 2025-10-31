package dev.korryr.tubefetch.domain.model

data class DownloadRequest(
    val url: String,
    val quality: VideoQuality,
    val format: DownloadFormat,
    val title: String,
    val fileName: String
)