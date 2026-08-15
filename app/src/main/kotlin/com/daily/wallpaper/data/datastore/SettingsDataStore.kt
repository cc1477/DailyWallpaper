package com.daily.wallpaper.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daily.wallpaper.data.model.AlcyCategory
import com.daily.wallpaper.data.model.AppSettings
import com.daily.wallpaper.data.model.BingResolution
import com.daily.wallpaper.data.model.ChangeFrequency
import com.daily.wallpaper.data.model.HideStrategy
import com.daily.wallpaper.data.model.SourceType
import com.daily.wallpaper.data.model.WallpaperItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_wallpaper_settings")

class SettingsDataStore(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val ENABLED_SOURCES = stringPreferencesKey("enabled_sources_ordered")
        val ALCY_CATEGORY = stringPreferencesKey("alcy_category")
        val BING_RESOLUTION = stringPreferencesKey("bing_resolution")
        val LOCAL_FOLDER_URI = stringPreferencesKey("local_folder_uri")
        val LOCAL_INCLUDE_SUBDIRS = booleanPreferencesKey("local_include_subdirs")
        val LOCAL_SEQUENTIAL = booleanPreferencesKey("local_sequential")
        val LOCAL_AVOID_REPEAT = booleanPreferencesKey("local_avoid_repeat")

        val TARGET_HOME = booleanPreferencesKey("target_home")
        val TARGET_LOCK = booleanPreferencesKey("target_lock")

        val AUTO_CHANGE_ENABLED = booleanPreferencesKey("auto_change_enabled")
        val CHANGE_HOUR = intPreferencesKey("change_hour")
        val CHANGE_MINUTE = intPreferencesKey("change_minute")
        val CHANGE_FREQUENCY = stringPreferencesKey("change_frequency")

        val HOME_BLUR = intPreferencesKey("home_blur")
        val LOCK_BLUR = intPreferencesKey("lock_blur")

        val SAVE_TO_ALBUM = booleanPreferencesKey("save_to_album")
        val HIDE_STRATEGY = stringPreferencesKey("hide_strategy")

        val CURRENT_WALLPAPER = stringPreferencesKey("current_wallpaper_json")
        val WALLPAPER_HISTORY = stringPreferencesKey("wallpaper_history_json")
    }

    private fun parseSources(raw: String?): List<SourceType> {
        if (raw.isNullOrBlank()) return listOf(SourceType.BING)
        return raw.split(",")
            .mapNotNull { SourceType.fromName(it.trim()) }
            .ifEmpty { listOf(SourceType.BING) }
    }

    private fun serializeSources(sources: List<SourceType>): String =
        sources.joinToString(",") { it.name }

    private fun readSettings(prefs: Preferences): AppSettings = AppSettings(
        enabledSources = parseSources(prefs[Keys.ENABLED_SOURCES]),
        alcyCategory = AlcyCategory.fromParam(prefs[Keys.ALCY_CATEGORY] ?: "mp") ?: AlcyCategory.MP,
        bingResolution = BingResolution.fromName(prefs[Keys.BING_RESOLUTION] ?: BingResolution.UHD.name) ?: BingResolution.UHD,
        localFolderUri = prefs[Keys.LOCAL_FOLDER_URI],
        localIncludeSubdirs = prefs[Keys.LOCAL_INCLUDE_SUBDIRS] ?: false,
        localSequentialMode = prefs[Keys.LOCAL_SEQUENTIAL] ?: false,
        localAvoidRepeat = prefs[Keys.LOCAL_AVOID_REPEAT] ?: true,
        targetHome = prefs[Keys.TARGET_HOME] ?: true,
        targetLock = prefs[Keys.TARGET_LOCK] ?: true,
        autoChangeEnabled = prefs[Keys.AUTO_CHANGE_ENABLED] ?: false,
        changeHour = prefs[Keys.CHANGE_HOUR] ?: 6,
        changeMinute = prefs[Keys.CHANGE_MINUTE] ?: 0,
        changeFrequency = ChangeFrequency.fromName(prefs[Keys.CHANGE_FREQUENCY] ?: ChangeFrequency.DAILY.name) ?: ChangeFrequency.DAILY,
        homeBlurIntensity = prefs[Keys.HOME_BLUR] ?: 0,
        lockBlurIntensity = prefs[Keys.LOCK_BLUR] ?: 0,
        saveToAlbum = prefs[Keys.SAVE_TO_ALBUM] ?: false,
        hideStrategy = HideStrategy.fromName(prefs[Keys.HIDE_STRATEGY] ?: HideStrategy.NONE.name) ?: HideStrategy.NONE,
    )

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs -> readSettings(prefs) }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = readSettings(prefs)
            val updated = transform(current)
            prefs[Keys.ENABLED_SOURCES] = serializeSources(updated.enabledSources)
            prefs[Keys.ALCY_CATEGORY] = updated.alcyCategory.param
            prefs[Keys.BING_RESOLUTION] = updated.bingResolution.name
            updated.localFolderUri?.let { prefs[Keys.LOCAL_FOLDER_URI] = it }
            prefs[Keys.LOCAL_INCLUDE_SUBDIRS] = updated.localIncludeSubdirs
            prefs[Keys.LOCAL_SEQUENTIAL] = updated.localSequentialMode
            prefs[Keys.LOCAL_AVOID_REPEAT] = updated.localAvoidRepeat
            prefs[Keys.TARGET_HOME] = updated.targetHome
            prefs[Keys.TARGET_LOCK] = updated.targetLock
            prefs[Keys.AUTO_CHANGE_ENABLED] = updated.autoChangeEnabled
            prefs[Keys.CHANGE_HOUR] = updated.changeHour
            prefs[Keys.CHANGE_MINUTE] = updated.changeMinute
            prefs[Keys.CHANGE_FREQUENCY] = updated.changeFrequency.name
            prefs[Keys.HOME_BLUR] = updated.homeBlurIntensity
            prefs[Keys.LOCK_BLUR] = updated.lockBlurIntensity
            prefs[Keys.SAVE_TO_ALBUM] = updated.saveToAlbum
            prefs[Keys.HIDE_STRATEGY] = updated.hideStrategy.name
        }
    }

    // ── 壁纸状态持久化 ──

    val currentWallpaperFlow: Flow<WallpaperItem?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_WALLPAPER]?.let { gson.fromJson(it, WallpaperItem::class.java) }
    }

    val historyFlow: Flow<List<WallpaperItem>> = context.dataStore.data.map { prefs ->
        prefs[Keys.WALLPAPER_HISTORY]?.let { json ->
            val type = object : TypeToken<List<WallpaperItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } ?: emptyList()
    }

    suspend fun saveCurrentWallpaper(wallpaper: WallpaperItem?) {
        context.dataStore.edit { prefs ->
            if (wallpaper != null) {
                prefs[Keys.CURRENT_WALLPAPER] = gson.toJson(wallpaper)
            } else {
                prefs.remove(Keys.CURRENT_WALLPAPER)
            }
        }
    }

    suspend fun saveHistory(history: List<WallpaperItem>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_HISTORY] = gson.toJson(history)
        }
    }
}
