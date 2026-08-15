package com.daily.wallpaper.data.model

import java.util.UUID

/**
 * 一张壁纸的信息
 */
data class WallpaperItem(
    val id: String = UUID.randomUUID().toString(),
    val source: SourceType,
    val imageUrl: String,
    val title: String = "",
    val copyright: String = "",
    val localPath: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 应用全部设置，持久化在 DataStore 中
 */
data class AppSettings(
    // ── 图源 ──
    val enabledSources: List<SourceType> = listOf(SourceType.BING),
    val alcyCategory: AlcyCategory = AlcyCategory.MP,
    val bingResolution: BingResolution = BingResolution.UHD,
    val localFolderUri: String? = null,
    val localIncludeSubdirs: Boolean = false,
    val localSequentialMode: Boolean = false,
    val localAvoidRepeat: Boolean = true,

    // ── 更换目标 ──
    val targetHome: Boolean = true,
    val targetLock: Boolean = true,

    // ── 定时 ──
    val autoChangeEnabled: Boolean = false,
    val changeHour: Int = 6,
    val changeMinute: Int = 0,
    val changeFrequency: ChangeFrequency = ChangeFrequency.DAILY,

    // ── 模糊 ──
    val homeBlurIntensity: Int = 0,
    val lockBlurIntensity: Int = 0,

    // ── 保存 ──
    val saveToAlbum: Boolean = false,
    val hideStrategy: HideStrategy = HideStrategy.NONE,
)

/**
 * 当前壁纸及历史记录状态
 */
data class WallpaperState(
    val currentWallpaper: WallpaperItem? = null,
    val history: List<WallpaperItem> = emptyList(),
    val isChanging: Boolean = false,
    val message: String? = null,
)
