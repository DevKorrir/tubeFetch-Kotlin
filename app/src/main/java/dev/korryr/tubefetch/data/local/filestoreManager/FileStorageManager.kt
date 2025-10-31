package dev.korryr.tubefetch.data.local.filestoreManager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dev.korryr.tubefetch.domain.model.DownloadFormat
import java.io.File
import javax.inject.Inject

class FileStorageManager @Inject constructor(
    private val context: Context
) {

    fun saveFileToMediaStore(
        downloadedFile: File,
        fileName: String,
        format: DownloadFormat
    ): Uri? {
        return try {
            val collection = when (format) {
                DownloadFormat.MP3, DownloadFormat.M4A, DownloadFormat.WAV ->
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else ->
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, getRelativePath(format))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val contentUri = context.contentResolver.insert(collection, contentValues)
            contentUri?.let { uri ->
                // Copy file content
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    downloadedFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Update IS_PENDING to 0
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)

                // Delete temporary file
                downloadedFile.delete()

                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRelativePath(format: DownloadFormat): String {
        return when (format) {
            DownloadFormat.MP3, DownloadFormat.M4A, DownloadFormat.WAV ->
                Environment.DIRECTORY_MUSIC + "/TubeFetch"
            else ->
                Environment.DIRECTORY_MOVIES + "/TubeFetch"
        }
    }

    fun getDownloadsDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "TubeFetch"
        ).apply { mkdirs() }
    }
}