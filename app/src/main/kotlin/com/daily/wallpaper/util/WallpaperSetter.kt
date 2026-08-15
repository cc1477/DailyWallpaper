package com.daily.wallpaper.util

import android.app.WallpaperManager
import android.graphics.Bitmap
import com.daily.wallpaper.util.Log

class WallpaperSetter(private val wallpaperManager: WallpaperManager) {

    companion object {
        private const val TAG = "WallpaperSetter"
    }

    /**
     * 设置壁纸到指定目标
     * @return true 如果设置成功
     * @throws Exception 设置失败时抛出，携带详细错误信息
     */
    fun setWallpaper(bitmap: Bitmap, targetHome: Boolean, targetLock: Boolean): Boolean {
        val flags = when {
            targetHome && targetLock -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            targetHome -> WallpaperManager.FLAG_SYSTEM
            targetLock -> WallpaperManager.FLAG_LOCK
            else -> return false
        }

        Log.d(TAG, "setWallpaper: flags=$flags, home=$targetHome, lock=$targetLock, " +
            "bitmap=${bitmap.width}x${bitmap.height}")

        return try {
            wallpaperManager.setBitmap(bitmap, null, true, flags)
            Log.d(TAG, "setWallpaper: success with flags")
            true
        } catch (e: Exception) {
            Log.e(TAG, "setWallpaper: setBitmap with flags failed", e)
            // 某些设备/MIUI 不支持 LOCK flag，退回分别设置
            try {
                if (targetHome) {
                    wallpaperManager.setBitmap(bitmap)
                    Log.d(TAG, "setWallpaper: fallback setBitmap() success")
                    true
                } else if (targetLock) {
                    // 尝试仅 LOCK
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    Log.d(TAG, "setWallpaper: fallback LOCK-only success")
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                Log.e(TAG, "setWallpaper: fallback also failed", e2)
                throw e2
            }
        }
    }
}
