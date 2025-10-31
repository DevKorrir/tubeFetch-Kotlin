package dev.korryr.tubefetch.data.remote

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class YouTubeNativeService @Inject constructor(
    private val context: Context
) {
    private val TAG = "YouTubeNativeService"

    init {
        initializeYouTubeDL()
    }

    private fun initializeYouTubeDL() {
        try {
            YoutubeDL.getInstance().init(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YouTubeDL", e)
        }
    }

    suspend fun getVideoInfo(url: String): ApiResult<dev.korryr.tubefetch.domain.model.VideoInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--no-playlist")

            // FIX: Remove the second parameter or use null
            val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, null)
            val jsonOutput = response.out
            val json = JSONObject(jsonOutput)

            val videoInfo = parseVideoInfo(json)
            ApiResult.Success(videoInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video info", e)
            ApiResult.Error("Failed to get video info: ${e.message}", e)
        }
    }

    suspend fun downloadVideo(
        url: String,
        outputDir: File,
        format: String,
        quality: String
    ): ApiResult<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = YoutubeDLRequest(url)
            request.addOption("--no-playlist")

            // Set output template
            val outputTemplate = "${outputDir.absolutePath}/%(title)s.%(ext)s"
            request.addOption("-o", outputTemplate)

            // Set format based on quality and format type
            when {
                format == "mp3" -> {
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--audio-quality", "0") // Best quality
                }
                format == "mp4" -> {
                    when (quality) {
                        "AUTO" -> request.addOption("--format", "best[ext=mp4]")
                        "HD1080" -> request.addOption("--format", "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]")
                        "HD720" -> request.addOption("--format", "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]")
                        "SD480" -> request.addOption("--format", "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]")
                        else -> request.addOption("--format", "best[ext=mp4]")
                    }
                }
            }

            Log.d(TAG, "Starting download: $url")
            // FIX: Remove the second parameter or use null
            val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request, null)

            // Find the downloaded file
            val downloadedFile = findDownloadedFile(outputDir, response.out)
            if (downloadedFile != null && downloadedFile.exists()) {
                ApiResult.Success(downloadedFile)
            } else {
                ApiResult.Error("Download completed but file not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video", e)
            ApiResult.Error("Download failed: ${e.message}", e)
        }
    }

    private fun parseVideoInfo(json: JSONObject): dev.korryr.tubefetch.domain.model.VideoInfo {
        return dev.korryr.tubefetch.domain.model.VideoInfo(
            title = json.optString("title", "Unknown Title"),
            duration = formatDuration(json.optLong("duration", 0)),
            thumbnail = json.optString("thumbnail", ""),
            channelName = json.optString("uploader", "Unknown Channel"),
            viewCount = formatViewCount(json.optLong("view_count", 0)),
            uploadDate = json.optString("upload_date", ""),
            description = json.optString("description", ""),
            availableQualities = getAvailableQualities(json)
        )
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private fun formatViewCount(views: Long): String {
        return when {
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
    }

    private fun getAvailableQualities(json: JSONObject): List<VideoQuality> {
        val qualities = mutableListOf<VideoQuality>()
        qualities.add(VideoQuality.AUTO) // Always available

        // Parse available formats from JSON
        val formats = json.optJSONArray("formats")
        formats?.let {
            for (i in 0 until it.length()) {
                val format = it.getJSONObject(i)
                val height = format.optInt("height", 0)
                val formatNote = format.optString("format_note", "")

                when {
                    height >= 2160 -> qualities.addIfNotExists(VideoQuality.UHD2160)
                    height >= 1440 -> qualities.addIfNotExists(VideoQuality.QHD1440)
                    height >= 1080 -> qualities.addIfNotExists(VideoQuality.HD1080)
                    height >= 720 -> qualities.addIfNotExists(VideoQuality.HD720)
                    height >= 480 -> qualities.addIfNotExists(VideoQuality.SD480)
                    height >= 360 -> qualities.addIfNotExists(VideoQuality.SD360)
                }
            }
        }

        return qualities.distinct()
    }

    private fun <T> MutableList<T>.addIfNotExists(element: T) {
        if (!this.contains(element)) {
            this.add(element)
        }
    }

    private fun findDownloadedFile(outputDir: File, output: String): File? {
        // Parse the output to find the downloaded file path
        val lines = output.split("\n")
        for (line in lines) {
            if (line.contains("[download] Destination:")) {
                val path = line.substringAfter("[download] Destination:").trim()
                return File(path)
            }
            if (line.contains("[ffmpeg] Destination:")) {
                val path = line.substringAfter("[ffmpeg] Destination:").trim()
                return File(path)
            }
        }

        // Fallback: look for files in the output directory
        return outputDir.listFiles()?.firstOrNull { file ->
            file.isFile && (file.extension == "mp4" || file.extension == "mp3" || file.extension == "webm")
        }
    }
}