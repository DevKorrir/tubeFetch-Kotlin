package dev.korryr.tubefetch.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector

enum class DownloadFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val icon: ImageVector
) {
    MP4("Video (MP4)", "mp4", "video/mp4", Icons.Rounded.VideoFile),
    MP3("Audio (MP3)", "mp3", "audio/mpeg", Icons.Rounded.AudioFile),
    WEBM("Video (WebM)", "webm", "video/webm", Icons.Filled.Videocam),
    M4A("Audio (M4A)", "m4a", "audio/mp4", Icons.Filled.MusicNote),
    WAV("Audio (WAV)", "wav", "audio/wav", Icons.Filled.Audiotrack)
}