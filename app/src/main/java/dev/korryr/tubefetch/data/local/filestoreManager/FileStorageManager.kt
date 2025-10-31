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
    
    fun createFileUri(fileName: String, format: DownloadFormat): Uri? {
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
            }
            
            context.contentResolver.insert(collection, contentValues)
        } catch (e: Exception) {
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
    
    fun getFileOutputStream(uri: Uri) = context.contentResolver.openOutputStream(uri)
    
    fun fileExists(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}