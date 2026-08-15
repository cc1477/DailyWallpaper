package com.daily.wallpaper.util

import android.graphics.Bitmap
import com.daily.wallpaper.util.Log

class BlurProcessor {

    companion object {
        private const val TAG = "BlurProcessor"
    }

    /**
     * 对 Bitmap 应用高斯模糊
     * @param intensity 模糊强度 0-100（0 = 不模糊）
     * @return 模糊后的 Bitmap（原图不被修改）
     */
    fun blur(bitmap: Bitmap, intensity: Int): Bitmap {
        if (intensity <= 0) return bitmap

        // 将 0-100 映射到 1-25 的缩放因子
        val downScale = (intensity / 100f * 12f).toInt().coerceIn(1, 12)

        Log.d(TAG, "blur: intensity=$intensity, downScale=$downScale, src=${bitmap.width}x${bitmap.height}")

        return try {
            blurByScaling(bitmap, downScale)
        } catch (e: Exception) {
            Log.e(TAG, "blur failed, returning original", e)
            bitmap
        }
    }

    /**
     * 缩小 → 盒式模糊 → 放大回原尺寸
     * 简单可靠，不会数组越界，性能也好（在缩小后的小图上操作）
     */
    private fun blurByScaling(bitmap: Bitmap, downScale: Int): Bitmap {
        val scaledW = (bitmap.width / downScale).coerceAtLeast(1)
        val scaledH = (bitmap.height / downScale).coerceAtLeast(1)

        // 1. 缩小
        val small = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        Log.d(TAG, "blurByScaling: small=${small.width}x${small.height}")

        // 2. 在小图上做 3 轮半径1的盒式模糊
        val blurred = boxBlur(small, radius = 1, iterations = 3)

        // 3. 放大回原尺寸（双线性插值）
        val result = Bitmap.createScaledBitmap(blurred, bitmap.width, bitmap.height, true)

        // 回收中间 bitmap
        if (small !== bitmap) small.recycle()
        if (blurred !== small) blurred.recycle()

        return result
    }

    /**
     * 简单盒式模糊 — 水平 + 垂直各扫一遍，重复 iterations 次
     * 所有索引都用 coerceIn 保证不越界
     */
    private fun boxBlur(src: Bitmap, radius: Int, iterations: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val temp = IntArray(w * h)
        val div = radius * 2 + 1

        repeat(iterations) {
            // 水平方向
            for (y in 0 until h) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                // 初始化窗口
                for (dx in -radius..radius) {
                    val px = (dx).coerceIn(0, w - 1)
                    val pixel = pixels[y * w + px]
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                }

                for (x in 0 until w) {
                    temp[y * w + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)

                    // 滑动窗口：加入右边新像素，移除左边旧像素
                    val newX = (x + radius + 1).coerceIn(0, w - 1)
                    val oldX = (x - radius).coerceIn(0, w - 1)
                    val newPixel = pixels[y * w + newX]
                    val oldPixel = pixels[y * w + oldX]
                    rSum += ((newPixel shr 16) and 0xFF) - ((oldPixel shr 16) and 0xFF)
                    gSum += ((newPixel shr 8) and 0xFF) - ((oldPixel shr 8) and 0xFF)
                    bSum += (newPixel and 0xFF) - (oldPixel and 0xFF)
                }
            }

            // 垂直方向
            for (x in 0 until w) {
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (dy in -radius..radius) {
                    val py = dy.coerceIn(0, h - 1)
                    val pixel = temp[py * w + x]
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                }

                for (y in 0 until h) {
                    pixels[y * w + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)

                    val newY = (y + radius + 1).coerceIn(0, h - 1)
                    val oldY = (y - radius).coerceIn(0, h - 1)
                    val newPixel = temp[newY * w + x]
                    val oldPixel = temp[oldY * w + x]
                    rSum += ((newPixel shr 16) and 0xFF) - ((oldPixel shr 16) and 0xFF)
                    gSum += ((newPixel shr 8) and 0xFF) - ((oldPixel shr 8) and 0xFF)
                    bSum += (newPixel and 0xFF) - (oldPixel and 0xFF)
                }
            }
        }

        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }
}
