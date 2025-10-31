package dev.korryr.tubefetch.data.remote

enum class DownloadFilter(val displayName: String) {
    ALL("All"),
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    AUDIO("Audio Only"),
    VIDEO("Video Only")
}