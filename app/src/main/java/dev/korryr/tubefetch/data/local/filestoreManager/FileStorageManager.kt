package dev.korryr.tubefetch.data.local.filestoreManager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern scoped-storage flow: insert into Downloads collection with RELATIVE_PATH
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

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

                    // Mark as complete
                    val updateValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, updateValues, null, null)

                    // Delete temporary file
                    downloadedFile.delete()

                    uri
                }
            } else {
                // Legacy flow for pre-Android 10: write into public Downloads/TubeFetch
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "TubeFetch"
                ).apply { mkdirs() }

                val destFile = File(downloadsDir, fileName)
                downloadedFile.copyTo(destFile, overwrite = true)
                downloadedFile.delete()

                // Insert into MediaStore Audio/Video so media scanners & apps can see it
                val collection = when (format) {
                    DownloadFormat.MP3, DownloadFormat.M4A, DownloadFormat.WAV ->
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else ->
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val values = ContentValues().apply {
                    @Suppress("DEPRECATION")
                    put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                }

                // We don't strictly need the returned Uri for legacy, but return it
                context.contentResolver.insert(collection, values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRelativePath(format: DownloadFormat): String {
        // Put all downloads under the public Downloads/TubeFetch folder
        return Environment.DIRECTORY_DOWNLOADS + "/TubeFetch"
    }

    fun getDownloadsDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "TubeFetch"
        ).apply { mkdirs() }
    }
}