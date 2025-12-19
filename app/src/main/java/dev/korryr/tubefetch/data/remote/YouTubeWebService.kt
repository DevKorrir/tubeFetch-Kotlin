package dev.korryr.tubefetch.data.remote

import dev.korryr.tubefetch.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeWebService {
    @GET("/v2/video/details")
    suspend fun getVideoDetails(
        @Query("videoId") videoId: String,
        @Query("urlAccess") urlAccess: String = "normal",
        @Query("videos") videos: String = "auto",
        @Query("audios") audios: String = "auto"
    ): VideoInfoResponse


}