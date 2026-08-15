package com.daily.wallpaper

import android.app.Application
import android.app.WallpaperManager
import com.daily.wallpaper.data.datastore.SettingsDataStore
import com.daily.wallpaper.data.repository.WallpaperRepository
import com.daily.wallpaper.util.AlbumSaver
import com.daily.wallpaper.util.BlurProcessor
import com.daily.wallpaper.util.WallpaperSetter
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class DailyWallpaperApp : Application() {

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var wallpaperRepository: WallpaperRepository
        private set

    lateinit var wallpaperSetter: WallpaperSetter
        private set

    lateinit var blurProcessor: BlurProcessor
        private set

    lateinit var albumSaver: AlbumSaver
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        settingsDataStore = SettingsDataStore(this)
        wallpaperSetter = WallpaperSetter(WallpaperManager.getInstance(this))
        blurProcessor = BlurProcessor()
        albumSaver = AlbumSaver(this)
        wallpaperRepository = WallpaperRepository(this, okHttpClient)
    }

    companion object {
        lateinit var instance: DailyWallpaperApp
            private set
    }
}
