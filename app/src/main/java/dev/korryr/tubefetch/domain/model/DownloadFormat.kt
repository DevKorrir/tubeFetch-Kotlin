package dev.korryr.tubefetch.domain.model

enum class DownloadFormat(val displayName: String, val extension: String, val mimeType: String) {
    MP4("Video (MP4)", "mp4", "video/mp4"),
    MP3("Audio (MP3)", "mp3", "audio/mpeg"),
    WEBM("Video (WebM)", "webm", "video/webm"),
    M4A("Audio (M4A)", "m4a", "audio/mp4"),
    WAV("Audio (WAV)", "wav", "audio/wav")
}