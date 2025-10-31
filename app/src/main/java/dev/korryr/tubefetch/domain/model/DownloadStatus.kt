package dev.korryr.tubefetch.domain.model

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, PAUSED, QUEUED, PROCESSING
}