package com.daily.wallpaper.data.model

/**
 * 壁纸图源类型
 */
enum class SourceType(val displayName: String) {
    BING("Bing 每日壁纸"),
    ALCY("栗次元"),
    LOCAL("本地文件夹");

    companion object {
        fun fromName(name: String): SourceType? =
            entries.find { it.name == name }
    }
}

/**
 * Bing 壁纸分辨率
 */
enum class BingResolution(
    val displayName: String,
    val suffix: String,
    val isPortrait: Boolean,
) {
    UHD("UHD 超清横屏（推荐）", "_UHD.jpg", false),
    FHD("1080p 横屏", "_1920x1080.jpg", false),
    PORTRAIT("竖屏 1080x1920", "_1080x1920.jpg", true);

    companion object {
        fun fromName(name: String): BingResolution? =
            entries.find { it.name == name }
    }
}

/**
 * 栗次元壁纸分类（已排除头像类 tx/lai/xhl 和测试类 acg）
 */
enum class AlcyCategory(
    val param: String,
    val displayName: String,
    val isVertical: Boolean,
) {
    MP("mp", "移动竖图", true),
    AIMP("aimp", "AI 竖图", true),
    YSMP("ysmp", "原神竖图", true),
    MOEMP("moemp", "萌版竖图", true),
    PIXIV("pixiv", "PIXIV 随机", true),
    PC("pc", "PC 横图", false),
    MOE("moe", "萌版横图", false),
    FJ("fj", "风景横图", false),
    BD("bd", "白底横图", false),
    YS("ys", "原神横图", false);

    companion object {
        fun fromParam(param: String): AlcyCategory? =
            entries.find { it.param == param }
    }
}

/**
 * 自动更换频率
 */
enum class ChangeFrequency(val displayName: String, val days: Int) {
    DAILY("每日", 1),
    EVERY_2_DAYS("每 2 日", 2),
    WEEKLY("每周", 7);

    companion object {
        fun fromName(name: String): ChangeFrequency? =
            entries.find { it.name == name }
    }
}

/**
 * 相册隐藏策略
 */
enum class HideStrategy(val displayName: String) {
    NONE("不隐藏（默认）"),
    NOMEDIA(".nomedia 标记隐藏");

    companion object {
        fun fromName(name: String): HideStrategy? =
            entries.find { it.name == name }
    }
}

/**
 * 壁纸更换目标
 */
enum class WallpaperTarget {
    HOME, LOCK, BOTH
}
