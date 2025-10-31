package dev.korryr.tubefetch.data.remote

import retrofit2.http.*

// We'll use a YouTube API wrapper service
interface VideoService {
    
    @GET("info")
    suspend fun getVideoInfo(
        @Query("url") url: String,
        @Query("key") apiKey: String = BuildConfig.YOUTUBE_API_KEY
    ): VideoInfoResponse

    @GET("download")
    suspend fun getDownloadUrl(
        @Query("url") url: String,
        @Query("format") format: String,
        @Query("quality") quality: String,
        @Query("key") apiKey: String = BuildConfig.YOUTUBE_API_KEY
    ): DownloadUrlResponse

    companion object {
        const val BASE_URL = "https://your-youtube-api-service.com/" // We'll set this up
    }
}