package dev.korryr.tubefetch.data.remote

data class VideoInfoResponse(
    val errorId: String,
    val type: String?,
    val id: String?,
    val title: String?,
    val description: String?,
    val channel: ChannelInfo?,
    val lengthSeconds: Int?,
    val viewCount: Long?,
    val publishedTimeText: String?,
    val thumbnails: List<Thumbnail>?,
    val videos: VideosSection?,
    val audios: AudiosSection?
) {
    data class ChannelInfo(
        val name: String?
    )

    data class Thumbnail(
        val url: String?,
        val width: Int?,
        val height: Int?
    )

    data class VideosSection(
        val items: List<VideoItem>?
    )

    data class VideoItem(
        val url: String?,
        val lengthMs: Long?,
        val mimeType: String?,
        val extension: String?,
        val size: Long?,
        val sizeText: String?,
        val hasAudio: Boolean?,
        val quality: String?,
        val width: Int?,
        val height: Int?
    )

    data class AudiosSection(
        val items: List<AudioItem>?
    )

    data class AudioItem(
        val url: String?,
        val lengthMs: Long?,
        val mimeType: String?,
        val extension: String?,
        val size: Long?,
        val sizeText: String?,
        val isDrc: Boolean?
    )
}