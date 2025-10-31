package dev.korryr.tubefetch.data.remote

data class VideoInfoResponse(
    val title: String,
    val duration: String,
    val thumbnail: String,
    val channel: ChannelInfo,
    val viewCount: String,
    val uploadDate: String,
    val description: String,
    val formats: List<FormatInfo>
) {
    data class ChannelInfo(val name: String)
    data class FormatInfo(val quality: String, val qualityLabel: String)
}