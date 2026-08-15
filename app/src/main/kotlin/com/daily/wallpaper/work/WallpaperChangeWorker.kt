package com.daily.wallpaper.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.daily.wallpaper.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daily.wallpaper.DailyWallpaperApp
import com.daily.wallpaper.MainActivity
import com.daily.wallpaper.R
import com.daily.wallpaper.data.model.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WallpaperChangeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "WallpaperWorker"
        const val CHANNEL_ID = "wallpaper_change_channel"
        const val CHANNEL_NAME = "壁纸更换通知"
        const val WORK_NAME = "daily_wallpaper_change"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? DailyWallpaperApp
            ?: return Result.failure()

        val settings = app.settingsDataStore.settingsFlow.first()
        Log.i(TAG, "doWork: sources=${settings.enabledSources}, home=${settings.targetHome}, lock=${settings.targetLock}")

        if (settings.enabledSources.isEmpty()) {
            Log.w(TAG, "doWork: no enabled sources, skipping")
            return Result.success()
        }

        var lastError: Exception? = null

        for (source in settings.enabledSources) {
            for (attempt in 1..3) {
                try {
                    Log.d(TAG, "doWork: source=${source.displayName}, attempt=$attempt")

                    val wallpaper = app.wallpaperRepository.fetchWallpaper(
                        source = source,
                        alcyCategory = settings.alcyCategory,
                        bingResolution = settings.bingResolution,
                        localFolderUri = settings.localFolderUri,
                        localIncludeSubdirs = settings.localIncludeSubdirs,
                    ).getOrThrow()

                    Log.d(TAG, "doWork: got wallpaper url=${wallpaper.imageUrl}")

                    val bitmap = withContext(Dispatchers.IO) {
                        loadAndDecodeBitmap(app, wallpaper.imageUrl)
                    }
                        ?: throw IllegalStateException("图片下载或解码失败")

                    Log.d(TAG, "doWork: bitmap ${bitmap.width}x${bitmap.height}")

                    val homeBitmap = if (settings.homeBlurIntensity > 0) {
                        app.blurProcessor.blur(bitmap, settings.homeBlurIntensity)
                    } else {
                        bitmap
                    }
                    val lockBitmap = if (settings.lockBlurIntensity > 0) {
                        app.blurProcessor.blur(bitmap, settings.lockBlurIntensity)
                    } else {
                        bitmap
                    }

                    val target = when {
                        settings.targetHome && settings.targetLock -> WallpaperTarget.BOTH
                        settings.targetHome -> WallpaperTarget.HOME
                        settings.targetLock -> WallpaperTarget.LOCK
                        else -> WallpaperTarget.BOTH
                    }

                    var success = false
                    withContext(Dispatchers.IO) {
                        when (target) {
                            WallpaperTarget.BOTH -> {
                                val h = try {
                                    app.wallpaperSetter.setWallpaper(homeBitmap, true, false)
                                } catch (e: Exception) { Log.e(TAG, "set HOME failed", e); false }
                                val l = try {
                                    app.wallpaperSetter.setWallpaper(lockBitmap, false, true)
                                } catch (e: Exception) { Log.e(TAG, "set LOCK failed", e); false }
                                success = h || l
                            }
                            WallpaperTarget.HOME -> {
                                success = try {
                                    app.wallpaperSetter.setWallpaper(homeBitmap, true, false)
                                } catch (e: Exception) { Log.e(TAG, "set HOME failed", e); false }
                            }
                            WallpaperTarget.LOCK -> {
                                success = try {
                                    app.wallpaperSetter.setWallpaper(lockBitmap, false, true)
                                } catch (e: Exception) { Log.e(TAG, "set LOCK failed", e); false }
                            }
                        }
                    }

                    if (success) {
                        if (settings.saveToAlbum) {
                            withContext(Dispatchers.IO) {
                                app.albumSaver.saveToAlbum(
                                    bitmap, settings.hideStrategy
                                )
                            }
                        }
                        showNotification("壁纸更换成功", "来源: ${source.displayName}")
                        Log.i(TAG, "doWork: success!")
                        return Result.success()
                    } else {
                        throw IllegalStateException("系统拒绝设置壁纸")
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "doWork: source=${source.displayName} attempt=$attempt failed", e)
                    kotlinx.coroutines.delay(2000L * attempt)
                }
            }
        }

        showNotification("壁纸更换失败", lastError?.message ?: lastError?.javaClass?.simpleName ?: "未知错误")
        return Result.retry()
    }

    private fun loadAndDecodeBitmap(
        app: DailyWallpaperApp,
        url: String,
    ): android.graphics.Bitmap? {
        return try {
            val uri = Uri.parse(url)
            val inputStream = app.wallpaperRepository.openInputStream(uri)
            val buffer = inputStream.readBytes()
            inputStream.close()
            Log.d(TAG, "loadAndDecodeBitmap: ${buffer.size} bytes")

            if (buffer.isEmpty()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(buffer, 0, buffer.size, bounds)

            if (bounds.outWidth <= 0) return null

            val targetSize = 2048
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > targetSize || bounds.outHeight / sampleSize > targetSize) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(buffer, 0, buffer.size, decodeOptions)
        } catch (e: Exception) {
            Log.e(TAG, "loadAndDecodeBitmap failed", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "loadAndDecodeBitmap OOM", e)
            null
        }
    }

    private fun showNotification(title: String, message: String) {
        val context = applicationContext
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
