package dev.korryr.tubefetch.data.remote

import dev.korryr.tubefetch.BuildConfig
import dev.korryr.tubefetch.domain.model.ApiResult
import dev.korryr.tubefetch.domain.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

class YouTubeWebServiceImpl @Inject constructor() {
    private val service: YouTubeWebService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-RapidAPI-Key", BuildConfig.YOUTUBE_API_KEY)
                    .addHeader("X-RapidAPI-Host", "youtube-downloader-api.p.rapidapi.com")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(YouTubeWebService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(YouTubeWebService::class.java)
    }

    suspend fun getVideoInfo(url: String): ApiResult<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val response = service.getVideoInfo(url)
            val videoInfo = response.toVideoInfo()
            ApiResult.Success(videoInfo)
        } catch (e: Exception) {
            ApiResult.Error("Failed to get video info: ${e.message}")
        }
    }
    suspend fun getDownloadUrl(
        url: String,
        format: String,
        quality: String
    ): ApiResult<DownloadUrlResponse> = withContext(Dispatchers.IO) {
        try {
            val response = service.getDownloadUrl(url, format, quality)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error("Failed to get download URL: ${e.message}")
        }
    }

    private fun VideoInfoResponse.toVideoInfo(): dev.korryr.tubefetch.domain.model.VideoInfo {
        return dev.korryr.tubefetch.domain.model.VideoInfo(
            title = title,
            duration = duration,
            thumbnail = thumbnail,
            channelName = channel.name,
            viewCount = viewCount,
            uploadDate = uploadDate,
            description = description,
            availableQualities = formats.mapNotNull { format ->
                // Map qualities to your domain model
                when (format.qualityLabel) {
                    "1080p" -> dev.korryr.tubefetch.domain.model.VideoQuality.HD1080
                    "720p" -> dev.korryr.tubefetch.domain.model.VideoQuality.HD720
                    "480p" -> dev.korryr.tubefetch.domain.model.VideoQuality.SD480
                    "360p" -> dev.korryr.tubefetch.domain.model.VideoQuality.SD360
                    else -> null
                }
            }
        )
    }
}