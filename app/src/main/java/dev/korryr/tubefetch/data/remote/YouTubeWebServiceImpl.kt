package dev.korryr.tubefetch.data.remote

import android.annotation.SuppressLint
import android.util.Log
import dev.korryr.tubefetch.BuildConfig
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.VideoInfo
import dev.korryr.tubefetch.domain.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class YouTubeWebServiceImpl @Inject constructor(
    private val service: YouTubeWebService
) {

    suspend fun getVideoInfo(url: String): ApiResult<VideoInfo> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url)
            ?: return@withContext ApiResult.Error("Invalid YouTube URL")

        // Debug logging
        Log.d("YouTubeWebService", "Fetching video info for ID: $videoId")
        Log.d("YouTubeWebService", "API Base URL: ${BuildConfig.YOUTUBE_BASE_URL}")
        Log.d("YouTubeWebService", "API Key: ${BuildConfig.YOUTUBE_API_KEY.take(10)}...")

        try {
            val response = service.getVideoDetails(
                videoId = videoId,
                urlAccess = "normal",
                videos = "auto",
                audios = "auto"
            )

            if (response.errorId != "Success") {
                return@withContext ApiResult.Error("API error: ${response.errorId}")
            }

            ApiResult.Success(response.toVideoInfo())
        } catch (e: retrofit2.HttpException) {
            ApiResult.Error("HTTP ${e.code()}: ${e.message()}")
        } catch (e: Exception) {
            ApiResult.Error("Failed to get video info: ${e.message}")
        }
    }

    suspend fun getDownloadUrl(
        url: String,
        format: String,
        quality: VideoQuality?
    ): ApiResult<DownloadUrlResponse> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url)
            ?: return@withContext ApiResult.Error("Invalid YouTube URL")

        try {
            val response = service.getVideoDetails(
                videoId = videoId,
                urlAccess = "normal",
                videos = "auto",
                audios = "auto"
            )

            if (response.errorId != "Success") {
                return@withContext ApiResult.Error("API error: ${response.errorId}")
            }

            val isAudioFormat = when (format.lowercase()) {
                "mp3", "m4a", "wav" -> true
                else -> false
            }

            val downloadStream = if (isAudioFormat) {
                val audioItems = response.audios?.items.orEmpty()
                chooseAudioStream(audioItems, format)
            } else {
                val videoItems = response.videos?.items.orEmpty()
                chooseVideoStream(videoItems, format, quality)
            }

            if (downloadStream == null) {
                return@withContext ApiResult.Error("No matching stream found for format $format")
            }

            val (urlToDownload, quality, extension) = when (downloadStream) {
                is VideoInfoResponse.VideoItem -> {
                    val streamUrl = downloadStream.url.orEmpty()
                    if (streamUrl.isBlank()) {
                        return@withContext ApiResult.Error("No URL available for selected video stream")
                    }
                    Triple(
                        streamUrl,
                        downloadStream.quality ?: "unknown",
                        downloadStream.extension ?: format
                    )
                }
                is VideoInfoResponse.AudioItem -> {
                    val streamUrl = downloadStream.url.orEmpty()
                    if (streamUrl.isBlank()) {
                        return@withContext ApiResult.Error("No URL available for selected audio stream")
                    }
                    Triple(
                        streamUrl,
                        "audio",
                        downloadStream.extension ?: format
                    )
                }
                else -> {
                    return@withContext ApiResult.Error("Unsupported stream type for format $format")
                }
            }

            ApiResult.Success(
                DownloadUrlResponse(
                    url = urlToDownload,
                    title = response.title ?: "YouTube Video",
                    format = extension,
                    quality = quality
                )
            )
        } catch (e: retrofit2.HttpException) {
            ApiResult.Error("HTTP ${e.code()}: ${e.message()}")
        } catch (e: Exception) {
            ApiResult.Error("Failed to get download URL: ${e.message}")
        }
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("""v=([\w-]+)"""),
            Regex("""youtu\.be/([\w-]+)"""),
            Regex("""embed/([\w-]+)""")
        )


        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }

        return null
    }

    private fun VideoInfoResponse.toVideoInfo(): dev.korryr.tubefetch.domain.model.VideoInfo {
        val durationText = formatDuration(lengthSeconds)

        val thumbnailUrl = thumbnails
            ?.maxByOrNull { it.width ?: 0 }
            ?.url
            ?: ""

        val qualities = videos?.items
            ?.mapNotNull { it.quality }
            ?.mapNotNull { qualityString ->
                when (qualityString) {
                    "2160p", "4K" -> dev.korryr.tubefetch.domain.model.VideoQuality.UHD4K
                    "1440p" -> dev.korryr.tubefetch.domain.model.VideoQuality.QHD1440
                    "1080p" -> dev.korryr.tubefetch.domain.model.VideoQuality.HD1080
                    "720p" -> dev.korryr.tubefetch.domain.model.VideoQuality.HD720
                    "480p" -> dev.korryr.tubefetch.domain.model.VideoQuality.SD480
                    "360p" -> dev.korryr.tubefetch.domain.model.VideoQuality.SD360
                    else -> null
                }
            }
            ?.distinct()
            ?: emptyList()

        return dev.korryr.tubefetch.domain.model.VideoInfo(
            title = title ?: "",
            duration = durationText,
            thumbnail = thumbnailUrl,
            channelName = channel?.name ?: "",
            viewCount = viewCount?.toString() ?: "0",
            uploadDate = publishedTimeText ?: "",
            description = description ?: "",
            availableQualities = qualities
        )
    }

    private fun formatDuration(lengthSeconds: Int?): String {
        if (lengthSeconds == null || lengthSeconds <= 0) return ""

        val hours = lengthSeconds / 3600
        val minutes = (lengthSeconds % 3600) / 60
        val seconds = lengthSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun chooseVideoStream(
        items: List<VideoInfoResponse.VideoItem>,
        desiredExtension: String,
        desiredQuality: VideoQuality?
    ): VideoInfoResponse.VideoItem? {
        if (items.isEmpty()) return null

        val normalizedExt = desiredExtension.lowercase()
        val extensionFiltered = items.filter { it.extension?.equals(normalizedExt, ignoreCase = true) == true }
        val candidates = if (extensionFiltered.isNotEmpty()) extensionFiltered else items

        val qualitySpecific = when (desiredQuality) {
            null, VideoQuality.AUTO -> emptyList()
            VideoQuality.SD360 -> filterByQuality(candidates, listOf("360p"))
            VideoQuality.SD480 -> filterByQuality(candidates, listOf("480p"))
            VideoQuality.HD720 -> filterByQuality(candidates, listOf("720p"))
            VideoQuality.HD1080 -> filterByQuality(candidates, listOf("1080p"))
            VideoQuality.QHD1440 -> filterByQuality(candidates, listOf("1440p"))
            VideoQuality.UHD2160, VideoQuality.UHD4K -> filterByQuality(candidates, listOf("2160p", "4K"))
        }

        val listToPickFrom = if (qualitySpecific.isNotEmpty()) qualitySpecific else candidates

        // Prefer the highest resolution available among the remaining candidates
        return listToPickFrom.maxByOrNull { it.height ?: 0 }
    }

    private fun filterByQuality(
        items: List<VideoInfoResponse.VideoItem>,
        labels: List<String>
    ): List<VideoInfoResponse.VideoItem> {
        if (labels.isEmpty()) return emptyList()
        val normalized = labels.map { it.lowercase() }
        return items.filter { item ->
            val q = item.quality?.lowercase()
            q != null && normalized.contains(q)
        }
    }

    private fun chooseAudioStream(
        items: List<VideoInfoResponse.AudioItem>,
        @Suppress("UNUSED_PARAMETER") desiredExtension: String
    ): VideoInfoResponse.AudioItem? {
        // RapidAPI typically returns M4A for audio; prefer any available stream
        return items.firstOrNull()
    }
}
