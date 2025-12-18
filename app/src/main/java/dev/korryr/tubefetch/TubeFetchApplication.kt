package dev.korryr.tubefetch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.korryr.tubefetch.worker.DownloadWorker

@HiltAndroidApp
class TubeFetchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadWorker.createNotificationChannel(this)
    }
}
