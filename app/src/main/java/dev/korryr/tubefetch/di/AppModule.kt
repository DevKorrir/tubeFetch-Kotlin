package dev.korryr.tubefetch.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.korryr.tubefetch.BuildConfig
import dev.korryr.tubefetch.data.local.db.AppDatabase
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.remote.YouTubeWebService
import dev.korryr.tubefetch.data.repo.VideoRepositoryImpl
import dev.korryr.tubefetch.domain.repository.VideoRepository
import dev.korryr.tubefetch.domain.tracker.DownloadTracker
import dev.korryr.tubefetch.utils.PermissionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFileStorageManager(@ApplicationContext context: Context): FileStorageManager {
        return FileStorageManager(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", BuildConfig.YOUTUBE_API_KEY)
                    .addHeader("x-rapidapi-host", BuildConfig.YOUTUBE_HOST)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.YOUTUBE_BASE_URL) // ensure trailing slash in keys.properties
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Provide the Retrofit interface (single source of truth)
    @Provides
    @Singleton
    fun provideYouTubeWebService(retrofit: Retrofit): YouTubeWebService {
        return retrofit.create(YouTubeWebService::class.java)
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        youTubeWebService: YouTubeWebService, // <--- interface (not impl)
        database: AppDatabase,
        fileStorageManager: FileStorageManager,
        @ApplicationContext context: Context
    ): VideoRepository {
        return VideoRepositoryImpl(
            youTubeWebService = youTubeWebService,
            downloadDao = database.downloadDao(),
            fileStorageManager = fileStorageManager,
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideDownloadTracker(repository: VideoRepository): DownloadTracker {
        return DownloadTracker(repository)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
