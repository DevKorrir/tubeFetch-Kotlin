# tubeFetch-Kotlin


## 🏗️ **Technology Stack & Architecture**

```
📱 Android App Architecture:
├── 🎯 Clean Architecture + MVVM
├── 🗃️ Room Database (for download history)
├── 🌐 Retrofit + OkHttp (for networking)
├── 💉 Hilt (Dependency Injection)
├── 🎨 Jetpack Compose (UI)
├── 🎵 MediaStore (for file storage)
├── 🔄 WorkManager (background downloads)
├── 📁 File System (actual file storage)
└── 🔐 BuildConfig (API keys management)
```

## 📁 **Project Structure**

Let me create the complete project structure:

### **1. Project Root Structure**
```
app/
├── src/main/
│   ├── java/dev/korryr/tubefetch/
│   │   ├── data/           # Data Layer
│   │   ├── domain/         # Domain Layer  
│   │   ├── ui/            # Presentation Layer
│   │   ├── di/            # Dependency Injection
│   │   └── utils/         # Utilities
│   └── res/
├── build.gradle.kts
├── proguard-rules.pro
└── keys.properties        # API keys (gitignored)
```

## 🔐 RapidAPI YouTube Media Downloader – Developer Notes

This project uses the **YouTube Media Downloader** API on RapidAPI as the backend for analyzing and downloading YouTube videos.

### 1. Where to put secrets

- **File:** `keys.properties` (project root, gitignored)
- **Required keys:**
  - `YOUTUBE_API_KEY=...`
  - `YOUTUBE_BASE_URL=https://youtube-media-downloader.p.rapidapi.com/v2/`  
    (keep the trailing `/` – Retrofit requires it)
  - `YOUTUBE_HOST=youtube-media-downloader.p.rapidapi.com`

These values are loaded in **`app/build.gradle.kts`** into `BuildConfig`:

- `BuildConfig.YOUTUBE_API_KEY`
- `BuildConfig.YOUTUBE_BASE_URL`
- `BuildConfig.YOUTUBE_HOST`

After editing `keys.properties`, always **Sync Gradle**.

### 2. How networking is wired

- **Base Retrofit client & headers:** `app/src/main/java/dev/korryr/tubefetch/di/AppModule.kt`
  - Adds on every request:
    - `x-rapidapi-key: BuildConfig.YOUTUBE_API_KEY`
    - `x-rapidapi-host: BuildConfig.YOUTUBE_HOST`
  - Uses `BuildConfig.YOUTUBE_BASE_URL` as `.baseUrl(...)`.

- **Retrofit interface for RapidAPI:** `data/remote/YouTubeWebService.kt`
  - Main endpoint used:

    ```kotlin
    @GET("/v2/video/details")
    suspend fun getVideoDetails(
        @Query("videoId") videoId: String,
        @Query("urlAccess") urlAccess: String = "normal",
        @Query("videos") videos: String = "auto",
        @Query("audios") audios: String = "auto"
    ): VideoInfoResponse
    ```

- **Wrapper around Retrofit + mapping:** `data/remote/YouTubeWebServiceImpl.kt`
  - Extracts `videoId` from any YouTube URL (watch, youtu.be, embed).
  - Calls `getVideoDetails(...)`.
  - Maps `VideoInfoResponse` → domain `VideoInfo`.
  - Picks the correct stream URL from `videos.items` / `audios.items` for downloads.

- **Repository using the wrapper:** `data/repo/VideoRepositoryImpl.kt`
  - `analyzeVideo(url)` → `youTubeWebService.getVideoInfo(url)`.
  - `downloadVideo(request)` → `youTubeWebService.getDownloadUrl(request.url, request.format.extension)` → downloads the returned URL with OkHttp.

### 3. Rotating / changing the RapidAPI key

1. Get the new key from the RapidAPI dashboard.
2. Update `YOUTUBE_API_KEY` in `keys.properties`.
3. Sync Gradle and rebuild.

Nothing else in code needs to change as long as you stay on the same RapidAPI API (same host and base URL).

### 4. Updating endpoints or migrating API provider

- **If RapidAPI changes paths (e.g. new version):**
  - Update `YOUTUBE_BASE_URL` in `keys.properties` to the new base.
  - Update paths / query params in `YouTubeWebService.kt` to match the new docs.
  - If the JSON response shape changes, adjust `VideoInfoResponse.kt` and the mapping in `YouTubeWebServiceImpl.toVideoInfo()` / `getDownloadUrl()`.

- **If you switch away from RapidAPI entirely:**
  - Replace the Retrofit interface in `YouTubeWebService.kt` with the new service’s endpoints.
  - Keep the **domain layer** contracts (`VideoRepository`, `VideoInfo`, `DownloadRequest`, etc.) the same to avoid touching the UI.
  - Update `AppModule.provideOkHttpClient` if headers or auth mechanism change.

### 5. Quick checklist when something breaks

If downloads or analysis stop working, verify in this order:

1. `keys.properties` exists locally and has **non-empty** values.
2. Your RapidAPI subscription is active and not rate-limited.
3. The sample curl from RapidAPI’s dashboard works with your key.
4. `YOUTUBE_BASE_URL`, endpoint path in `YouTubeWebService.kt`, and the curl URL all match.

Use Logcat’s HTTP logs (OkHttp interceptor) to see the exact URL, status code, and error message returned by RapidAPI.

