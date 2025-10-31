package dev.korryr.tubefetch.data.remote

data class DownloadUrlResponse(
    val url: String,
    val title: String,
    val format: String,
    val quality: String
)