package com.daily.wallpaper.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.daily.wallpaper.util.Log
import com.daily.wallpaper.data.model.HideStrategy
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlbumSaver(private val context: Context) {

    companion object {
        private const val TAG = "AlbumSaver"
    }

    fun saveToAlbum(
        bitmap: Bitmap,
        hideStrategy: HideStrategy = HideStrategy.NONE,
    ): Result<Uri> = runCatching {
        Log.d(TAG, "saveToAlbum: ${bitmap.width}x${bitmap.height}, strategy=$hideStrategy")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val displayName = "Wallpaper_$timeStamp.jpg"

        val relativePath = "${Environment.DIRECTORY_PICTURES}/DailyWallpaper"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStoreQ(bitmap, displayName, relativePath, hideStrategy)
        } else {
            saveViaFile(bitmap, displayName, hideStrategy)
        }
    }

    private fun saveViaMediaStoreQ(
        bitmap: Bitmap,
        displayName: String,
        relativePath: String,
        hideStrategy: HideStrategy,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw IllegalStateException("无法创建 MediaStore 条目")

        context.contentResolver.openOutputStream(uri)?.use { os ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
        } ?: throw IllegalStateException("无法打开输出流")

        // .nomedia 策略：在壁纸目录下创建 .nomedia 文件，让相册忽略整个目录
        if (hideStrategy == HideStrategy.NOMEDIA) {
            ensureNomediaFile(relativePath)
        }

        Log.i(TAG, "saved: uri=$uri, strategy=$hideStrategy")
        return uri
    }

    private fun saveViaFile(
        bitmap: Bitmap,
        displayName: String,
        hideStrategy: HideStrategy,
    ): Uri {
        val picturesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "DailyWallpaper"
        )
        if (!picturesDir.exists()) picturesDir.mkdirs()

        val file = File(picturesDir, displayName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        if (hideStrategy == HideStrategy.NOMEDIA) {
            File(picturesDir, ".nomedia").createNewFile()
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: Uri.fromFile(file)
    }

    private fun ensureNomediaFile(relativePath: String) {
        try {
            val nomediaValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, ".nomedia")
                put(MediaStore.Images.Media.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, nomediaValues
            )?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(ByteArray(0))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureNomediaFile failed: ${e.message}")
        }
    }
}
