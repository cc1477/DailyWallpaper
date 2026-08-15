package com.daily.wallpaper.util

import com.daily.wallpaper.BuildConfig

/**
 * 统一日志工具，release 构建自动关闭所有日志
 */
object Log {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) android.util.Log.i(tag, msg)
    }

    fun w(tag: String, msg: String, e: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (e != null) android.util.Log.w(tag, msg, e) else android.util.Log.w(tag, msg)
        }
    }

    fun e(tag: String, msg: String, e: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (e != null) android.util.Log.e(tag, msg, e) else android.util.Log.e(tag, msg)
        }
    }
}
