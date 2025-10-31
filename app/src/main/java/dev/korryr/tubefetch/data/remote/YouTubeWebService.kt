package dev.korryr.tubefetch.data.remote

import dev.korryr.tubefetch.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeWebService {
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
        const val BASE_URL = BuildConfig.YOUTUBE_BASE_URL


    }
}