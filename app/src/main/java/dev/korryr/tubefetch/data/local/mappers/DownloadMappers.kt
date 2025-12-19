package dev.korryr.tubefetch.data.local.mappers

import dev.korryr.tubefetch.data.local.entity.DownloadEntity
import dev.korryr.tubefetch.domain.model.DownloadFormat
import dev.korryr.tubefetch.domain.model.DownloadItem
import dev.korryr.tubefetch.domain.model.DownloadStatus
import dev.korryr.tubefetch.domain.model.VideoQuality

fun DownloadItem.toEntity(): DownloadEntity {
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
        fileUri = fileUri
    )
}

fun DownloadEntity.toDownloadItem(): DownloadItem {
    return DownloadItem(
        id = id,
        title = title,
        duration = duration,
        thumbnail = thumbnail,
        status = try {
            DownloadStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            DownloadStatus.PENDING
        },
        progress = progress,
        fileSize = fileSize,
        downloadSpeed = downloadSpeed,
        quality = try {
            VideoQuality.valueOf(quality)
        } catch (e: IllegalArgumentException) {
            VideoQuality.AUTO
        },
        format = try {
            DownloadFormat.valueOf(format)
        } catch (e: IllegalArgumentException) {
            DownloadFormat.MP4
        },
        url = url,
        channelName = channelName,
        viewCount = viewCount,
        uploadDate = uploadDate,
        downloadPath = downloadPath,
        fileUri = fileUri,
        createdAt = createdAt
    )
}
