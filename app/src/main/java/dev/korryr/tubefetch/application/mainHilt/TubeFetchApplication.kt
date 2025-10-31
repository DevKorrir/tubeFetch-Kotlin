package dev.korryr.tubefetch.application.mainHilt

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import dev.korryr.tubefetch.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class TubeFetchApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (optional)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize YouTubeDL in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
               // YoutubeDL.getInstance().init(this@TubeFetchApplication)
                Timber.tag("TubeFetch").d("YouTubeDL initialized in Application")
            } catch (e: Exception) {
                Timber.tag("TubeFetch").e(e, "Failed to initialize YouTubeDL in Application")
            }
        }
    }
}