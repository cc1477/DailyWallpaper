package com.daily.wallpaper.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import com.daily.wallpaper.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.wallpaper.DailyWallpaperApp
import com.daily.wallpaper.data.model.AppSettings
import com.daily.wallpaper.data.model.WallpaperItem
import com.daily.wallpaper.data.model.WallpaperState
import com.daily.wallpaper.work.WallpaperChangeWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_HISTORY = 50
    }

    private val app = application as DailyWallpaperApp
    private val workManager = WorkManager.getInstance(application)

    val settings: StateFlow<AppSettings> = app.settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // ── 从 DataStore 恢复壁纸状态 ──
    private val _wallpaperState = MutableStateFlow(WallpaperState())
    val wallpaperState: StateFlow<WallpaperState> = _wallpaperState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // 启动时从 DataStore 恢复当前壁纸和历史
        viewModelScope.launch {
            app.settingsDataStore.currentWallpaperFlow.collect { saved ->
                _wallpaperState.value = _wallpaperState.value.copy(currentWallpaper = saved)
            }
        }
        viewModelScope.launch {
            app.settingsDataStore.historyFlow.collect { saved ->
                _wallpaperState.value = _wallpaperState.value.copy(history = saved)
            }
        }
    }

    fun onSnackbarShown() {
        _snackbarMessage.value = null
    }

    // ── 设置更新方法 ──

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            app.settingsDataStore.updateSettings(transform)
        }
    }

    fun toggleSource(source: com.daily.wallpaper.data.model.SourceType) {
        updateSettings { settings ->
            val current = settings.enabledSources.toMutableList()
            if (current.contains(source)) {
                current.remove(source)
            } else {
                current.add(source)
            }
            settings.copy(enabledSources = current)
        }
    }

    fun moveSource(source: com.daily.wallpaper.data.model.SourceType, up: Boolean) {
        updateSettings { settings ->
            val list = settings.enabledSources.toMutableList()
            val index = list.indexOf(source)
            if (index >= 0) {
                val targetIndex = if (up) index - 1 else index + 1
                if (targetIndex in list.indices) {
                    val temp = list[targetIndex]
                    list[targetIndex] = list[index]
                    list[index] = temp
                }
            }
            settings.copy(enabledSources = list)
        }
    }

    fun setLocalFolderUri(uri: Uri?) {
        updateSettings { it.copy(localFolderUri = uri?.toString()) }
    }

    fun persistFolderPermission(uri: Uri) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            app.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            Log.w(TAG, "persistFolderPermission failed: ${e.message}")
        }
    }

    fun getLocalImageCount(): Int {
        val settings = settings.value
        val uriStr = settings.localFolderUri ?: return 0
        return try {
            val treeUri = Uri.parse(uriStr)
            app.wallpaperRepository.scanLocalImages(treeUri, settings.localIncludeSubdirs).size
        } catch (e: Exception) {
            Log.w(TAG, "getLocalImageCount failed: ${e.message}")
            0
        }
    }

    // ── 壁纸操作 ──

    fun changeWallpaperNow() {
        viewModelScope.launch {
            val settings = settings.value
            if (settings.enabledSources.isEmpty()) {
                _snackbarMessage.value = "请先启用至少一个图源"
                return@launch
            }
            if (!settings.targetHome && !settings.targetLock) {
                _snackbarMessage.value = "请至少选择一个更换目标（桌面或锁屏）"
                return@launch
            }

            _wallpaperState.value = _wallpaperState.value.copy(isChanging = true)
            Log.i(TAG, "changeWallpaperNow: sources=${settings.enabledSources}, " +
                "home=${settings.targetHome}, lock=${settings.targetLock}, bingRes=${settings.bingResolution}")

            var lastError: String? = null
            for (source in settings.enabledSources) {
                try {
                    Log.d(TAG, "changeWallpaperNow: trying source=${source.displayName}")

                    val wallpaper = app.wallpaperRepository.fetchWallpaper(
                        source = source,
                        alcyCategory = settings.alcyCategory,
                        bingResolution = settings.bingResolution,
                        localFolderUri = settings.localFolderUri,
                        localIncludeSubdirs = settings.localIncludeSubdirs,
                    ).getOrThrow()

                    Log.d(TAG, "changeWallpaperNow: got wallpaper, url=${wallpaper.imageUrl}")

                    val bitmap = loadBitmap(wallpaper.imageUrl)
                    if (bitmap == null) {
                        lastError = "图片下载或解码失败"
                        Log.e(TAG, "changeWallpaperNow: loadBitmap returned null for ${wallpaper.imageUrl}")
                        continue
                    }
                    Log.d(TAG, "changeWallpaperNow: bitmap decoded ${bitmap.width}x${bitmap.height}")

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

                    var homeOk = false
                    var lockOk = false
                    withContext(Dispatchers.IO) {
                        if (settings.targetHome) {
                            homeOk = try {
                                app.wallpaperSetter.setWallpaper(homeBitmap, true, false)
                            } catch (e: Exception) {
                                Log.e(TAG, "setWallpaper HOME failed", e)
                                false
                            }
                        }
                        if (settings.targetLock) {
                            lockOk = try {
                                app.wallpaperSetter.setWallpaper(lockBitmap, false, true)
                            } catch (e: Exception) {
                                Log.e(TAG, "setWallpaper LOCK failed", e)
                                false
                            }
                        }
                    }

                    Log.i(TAG, "changeWallpaperNow: homeOk=$homeOk, lockOk=$lockOk")

                    if (homeOk || lockOk) {
                        // 去重：如果相同 imageUrl 已存在则移除旧的，再加到最前
                        val existing = _wallpaperState.value.history.filter { it.imageUrl != wallpaper.imageUrl }
                        val newHistory = listOf(wallpaper) + existing.take(MAX_HISTORY - 1)
                        _wallpaperState.value = _wallpaperState.value.copy(
                            currentWallpaper = wallpaper,
                            history = newHistory,
                            isChanging = false,
                        )
                        // 持久化
                        app.settingsDataStore.saveCurrentWallpaper(wallpaper)
                        app.settingsDataStore.saveHistory(newHistory)

                        _snackbarMessage.value = "壁纸更换成功 · ${source.displayName}"

                        if (settings.saveToAlbum) {
                            val result = withContext(Dispatchers.IO) {
                                app.albumSaver.saveToAlbum(
                                    bitmap, settings.hideStrategy
                                )
                            }
                            if (result.isFailure) {
                                Log.e(TAG, "saveToAlbum failed: ${result.exceptionOrNull()}")
                                _snackbarMessage.value = "壁纸已更换，但保存到相册失败"
                            }
                        }
                        return@launch
                    } else {
                        lastError = "设置壁纸失败（系统拒绝）"
                        Log.e(TAG, "changeWallpaperNow: both home and lock failed")
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: e.javaClass.simpleName
                    Log.e(TAG, "changeWallpaperNow: source=${source.displayName} failed", e)
                }
            }

            _wallpaperState.value = _wallpaperState.value.copy(isChanging = false)
            _snackbarMessage.value = "更换失败: $lastError"
        }
    }

    fun saveCurrentToAlbum() {
        viewModelScope.launch {
            val wallpaper = _wallpaperState.value.currentWallpaper
            if (wallpaper == null) {
                _snackbarMessage.value = "当前没有可保存的壁纸"
                return@launch
            }

            val bitmap = loadBitmap(wallpaper.imageUrl)
            if (bitmap == null) {
                _snackbarMessage.value = "图片加载失败，无法保存"
                return@launch
            }

            val settings = settings.value
            val result = withContext(Dispatchers.IO) {
                app.albumSaver.saveToAlbum(
                    bitmap, settings.hideStrategy
                )
            }
            _snackbarMessage.value = if (result.isSuccess) {
                "已保存到相册"
            } else {
                "保存失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private suspend fun loadBitmap(url: String): android.graphics.Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(url)
                val inputStream = app.wallpaperRepository.openInputStream(uri)
                val buffer = inputStream.readBytes()
                inputStream.close()
                Log.d(TAG, "loadBitmap: downloaded ${buffer.size} bytes from $url")

                if (buffer.isEmpty()) {
                    Log.e(TAG, "loadBitmap: empty response")
                    return@withContext null
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(buffer, 0, buffer.size, bounds)
                Log.d(TAG, "loadBitmap: image size=${bounds.outWidth}x${bounds.outHeight}, mime=${bounds.outMimeType}")

                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    Log.e(TAG, "loadBitmap: invalid image dimensions")
                    return@withContext null
                }

                val targetSize = 2048
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > targetSize ||
                    bounds.outHeight / sampleSize > targetSize
                ) {
                    sampleSize *= 2
                }
                Log.d(TAG, "loadBitmap: sampleSize=$sampleSize")

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.size, options)
                if (bmp == null) {
                    Log.e(TAG, "loadBitmap: decodeByteArray returned null")
                }
                bmp
            } catch (e: Exception) {
                Log.e(TAG, "loadBitmap: failed for $url", e)
                null
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "loadBitmap: OOM for $url", e)
                null
            }
        }

    // ── 定时任务调度 ──

    fun scheduleAutoChange() {
        val settings = settings.value
        if (!settings.autoChangeEnabled) {
            cancelAutoChange()
            return
        }

        val intervalDays = settings.changeFrequency.days.toLong()
        val request = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            intervalDays, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WallpaperChangeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelAutoChange() {
        workManager.cancelUniqueWork(WallpaperChangeWorker.WORK_NAME)
    }

    // ── 从历史记录设置壁纸 ──

    fun setWallpaperFromHistory(item: WallpaperItem) {
        viewModelScope.launch {
            val settings = settings.value
            _wallpaperState.value = _wallpaperState.value.copy(isChanging = true)
            Log.i(TAG, "setWallpaperFromHistory: ${item.title}, url=${item.imageUrl}")

            try {
                val bitmap = loadBitmap(item.imageUrl)
                if (bitmap == null) {
                    _snackbarMessage.value = "图片加载失败"
                    _wallpaperState.value = _wallpaperState.value.copy(isChanging = false)
                    return@launch
                }

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

                var homeOk = false
                var lockOk = false
                withContext(Dispatchers.IO) {
                    if (settings.targetHome) {
                        homeOk = try {
                            app.wallpaperSetter.setWallpaper(homeBitmap, true, false)
                        } catch (e: Exception) {
                            Log.e(TAG, "setWallpaperFromHistory HOME failed", e)
                            false
                        }
                    }
                    if (settings.targetLock) {
                        lockOk = try {
                            app.wallpaperSetter.setWallpaper(lockBitmap, false, true)
                        } catch (e: Exception) {
                            Log.e(TAG, "setWallpaperFromHistory LOCK failed", e)
                            false
                        }
                    }
                }

                if (homeOk || lockOk) {
                    _wallpaperState.value = _wallpaperState.value.copy(
                        currentWallpaper = item,
                        isChanging = false,
                    )
                    app.settingsDataStore.saveCurrentWallpaper(item)
                    _snackbarMessage.value = "壁纸设置成功 · ${item.title}"
                } else {
                    _wallpaperState.value = _wallpaperState.value.copy(isChanging = false)
                    _snackbarMessage.value = "设置壁纸失败"
                }
            } catch (e: Exception) {
                Log.e(TAG, "setWallpaperFromHistory failed", e)
                _wallpaperState.value = _wallpaperState.value.copy(isChanging = false)
                _snackbarMessage.value = "设置失败: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    // ── 删除历史记录 ──

    fun deleteHistoryItem(item: WallpaperItem) {
        viewModelScope.launch {
            val newHistory = _wallpaperState.value.history.filter { it.id != item.id }
            _wallpaperState.value = _wallpaperState.value.copy(history = newHistory)
            app.settingsDataStore.saveHistory(newHistory)
            _snackbarMessage.value = "已删除"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _wallpaperState.value = _wallpaperState.value.copy(history = emptyList())
            app.settingsDataStore.saveHistory(emptyList())
            _snackbarMessage.value = "历史已清空"
        }
    }

    // ── 从历史记录保存到相册 ──

    fun saveToAlbumFromHistory(item: WallpaperItem) {
        viewModelScope.launch {
            val bitmap = loadBitmap(item.imageUrl)
            if (bitmap == null) {
                _snackbarMessage.value = "图片加载失败，无法保存"
                return@launch
            }

            val settings = settings.value
            val result = withContext(Dispatchers.IO) {
                app.albumSaver.saveToAlbum(
                    bitmap, settings.hideStrategy
                )
            }
            _snackbarMessage.value = if (result.isSuccess) {
                "已保存到相册"
            } else {
                "保存失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
