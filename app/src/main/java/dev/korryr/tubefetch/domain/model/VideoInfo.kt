package dev.korryr.tubefetch.domain.model

data class VideoInfo(
    val title: String,
    val duration: String,
    val thumbnail: String,
    val channelName: String,
    val viewCount: String,
    val uploadDate: String,
    val description: String,
    val availableQualities: List<VideoQuality>
)