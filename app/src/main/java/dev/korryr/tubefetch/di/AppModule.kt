package dev.korryr.tubefetch.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.korryr.tubefetch.data.local.db.AppDatabase
import dev.korryr.tubefetch.data.local.filestoreManager.FileStorageManager
import dev.korryr.tubefetch.data.remote.YouTubeWebServiceImpl
import dev.korryr.tubefetch.data.repo.VideoRepositoryImpl
import dev.korryr.tubefetch.domain.repository.VideoRepository
import dev.korryr.tubefetch.domain.tracker.DownloadTracker
import dev.korryr.tubefetch.utils.PermissionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideYouTubeWebService(): YouTubeWebServiceImpl {
        return YouTubeWebServiceImpl()
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        youTubeWebService: YouTubeWebServiceImpl,
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
    fun provideFileStorageManager(@ApplicationContext context: Context): FileStorageManager {
        return FileStorageManager(context)
    }

    // ADD THIS: PermissionManager provider
    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideDownloadTracker(repository: VideoRepository): DownloadTracker {
        return DownloadTracker(repository)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}