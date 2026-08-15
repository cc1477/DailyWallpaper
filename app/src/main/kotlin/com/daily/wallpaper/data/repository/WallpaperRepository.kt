package com.daily.wallpaper.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.daily.wallpaper.util.Log
import com.daily.wallpaper.data.model.AlcyCategory
import com.daily.wallpaper.data.model.SourceType
import com.daily.wallpaper.data.model.WallpaperItem
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream

class WallpaperRepository(
    private val context: Context,
    private val client: OkHttpClient,
) {

    companion object {
        private const val TAG = "WallpaperRepo"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }

    /**
     * 从指定图源获取一张壁纸
     */
    suspend fun fetchWallpaper(
        source: SourceType,
        alcyCategory: AlcyCategory,
        bingResolution: com.daily.wallpaper.data.model.BingResolution = com.daily.wallpaper.data.model.BingResolution.UHD,
        localFolderUri: String? = null,
        localIncludeSubdirs: Boolean = false,
    ): Result<WallpaperItem> = runCatching {
        Log.d(TAG, "fetchWallpaper: source=$source, alcy=$alcyCategory, bingRes=$bingResolution")
        when (source) {
            SourceType.BING -> fetchBing(bingResolution)
            SourceType.ALCY -> fetchAlcy(alcyCategory)
            SourceType.LOCAL -> fetchLocal(
                localFolderUri ?: throw IllegalStateException("未选择本地壁纸文件夹"),
                localIncludeSubdirs,
            )
        }
    }

    /**
     * Bing 每日壁纸 — 根据分辨率设置构建图片 URL
     * Bing API 返回的 url 格式: /th?id=OHR.xxx_EN-US1234567890&rf=LaDigue_UHD.jpg
     * 我们提取 OHR ID，拼上目标分辨率后缀
     */
    private suspend fun fetchBing(resolution: com.daily.wallpaper.data.model.BingResolution): WallpaperItem = withContext(Dispatchers.IO) {
        val apiUrl = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN"
        Log.d(TAG, "fetchBing: requesting $apiUrl, resolution=$resolution")

        val request = Request.Builder().url(apiUrl)
            .header("User-Agent", UA)
            .build()
        val response = client.newCall(request).execute()
        Log.d(TAG, "fetchBing: HTTP ${response.code}")

        val body = response.body?.string()
            ?: throw IllegalStateException("Bing API 返回空响应")

        val json = JsonParser.parseString(body).asJsonObject
        val images = json.getAsJsonArray("images")
        val first = images[0].asJsonObject
        val rawUrl = first.get("url").asString
        val copyright = first.get("copyright")?.asString ?: ""
        val title = first.get("title")?.asString ?: "Bing 每日壁纸"

        // 从 url 中提取 OHR.xxx_EN-SE1234567890 部分
        // url 格式: /th?id=OHR.Name_EN-US1234567890&rf=LaDigue_UHD.jpg
        val ohrId = extractOhrId(rawUrl)
        val fullUrl = "https://www.bing.com/th?id=${ohrId}${resolution.suffix}"
        Log.d(TAG, "fetchBing: rawUrl=$rawUrl")
        Log.d(TAG, "fetchBing: finalUrl=$fullUrl, title=$title")

        WallpaperItem(
            source = SourceType.BING,
            imageUrl = fullUrl,
            title = title,
            copyright = copyright,
        )
    }

    /**
     * 从 Bing url 字段提取 OHR 基础 ID（去掉已有的分辨率后缀）
     * 输入: /th?id=OHR.ValleyDreams_ZH-CN9689713135_1920x1080.jpg&rf=...
     * 输出: OHR.ValleyDreams_ZH-CN9689713135
     */
    private fun extractOhrId(rawUrl: String): String {
        // 找到 id= 后面的部分
        val idStart = rawUrl.indexOf("id=")
        if (idStart < 0) return rawUrl
        val afterId = rawUrl.substring(idStart + 3)
        // 截到 & 之前
        val ampIndex = afterId.indexOf("&")
        val fullId = if (ampIndex >= 0) afterId.substring(0, ampIndex) else afterId
        // 去掉末尾的分辨率后缀（如 _1920x1080.jpg, _UHD.jpg, _800x480.jpg）
        val dotIndex = fullId.lastIndexOf(".jpg")
        if (dotIndex > 0) {
            val lastUnderscore = fullId.lastIndexOf("_", dotIndex)
            if (lastUnderscore > 0) {
                return fullId.substring(0, lastUnderscore)
            }
        }
        return fullId
    }

    /**
     * 栗次元 API — 网络请求必须在 IO 线程
     */
    private suspend fun fetchAlcy(category: AlcyCategory): WallpaperItem = withContext(Dispatchers.IO) {
        val url = "https://t.alcy.cc/json?${category.param}"
        Log.d(TAG, "fetchAlcy: requesting $url")

        val request = Request.Builder().url(url)
            .header("User-Agent", UA)
            .build()
        val response = client.newCall(request).execute()
        Log.d(TAG, "fetchAlcy: HTTP ${response.code}")

        val body = response.body?.string()
            ?: throw IllegalStateException("栗次元 API 返回空响应")
        Log.d(TAG, "fetchAlcy: body=$body")

        val json = JsonParser.parseString(body).asJsonObject
        val code = json.get("code")?.asInt ?: 200
        if (code != 200) {
            throw IllegalStateException("栗次元 API 返回错误码: $code")
        }
        val data = json.getAsJsonObject("data")
        val link = data.get("link").asString
        Log.d(TAG, "fetchAlcy: imageUrl=$link")

        WallpaperItem(
            source = SourceType.ALCY,
            imageUrl = link,
            title = category.displayName,
            copyright = "栗次元 · ${category.displayName}",
        )
    }

    /**
     * 本地文件夹随机选取一张图片
     */
    private suspend fun fetchLocal(folderUriStr: String, includeSubdirs: Boolean): WallpaperItem =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "fetchLocal: uri=$folderUriStr, subdirs=$includeSubdirs")
            val treeUri = Uri.parse(folderUriStr)
            val images = scanLocalImages(treeUri, includeSubdirs)
            Log.d(TAG, "fetchLocal: found ${images.size} images")

            if (images.isEmpty()) {
                throw IllegalStateException("本地文件夹内未找到图片")
            }

            val selected = images.random()
            val fileName = selected.lastPathSegment ?: "local_wallpaper"
            Log.d(TAG, "fetchLocal: selected=$fileName")

            WallpaperItem(
                source = SourceType.LOCAL,
                imageUrl = selected.toString(),
                localPath = selected.toString(),
                title = fileName,
                copyright = "本地文件 · $fileName",
            )
        }

    /**
     * 扫描本地文件夹中的图片文件
     */
    fun scanLocalImages(treeUri: Uri, includeSubdirs: Boolean): List<Uri> {
        val results = mutableListOf<Uri>()
        val contentResolver = context.contentResolver

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        fun scanDir(dirUri: Uri) {
            try {
                contentResolver.query(dirUri, projection, null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(0)
                        val name = cursor.getString(1)
                        val mime = cursor.getString(2)
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (includeSubdirs) {
                                val subDir = DocumentsContract.buildChildDocumentsUriUsingTree(
                                    treeUri, docId
                                )
                                scanDir(subDir)
                            }
                        } else if (isImageFile(name)) {
                            results.add(childUri)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "scanDir error: ${e.message}", e)
            }
        }

        scanDir(childrenUri)
        return results
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".bmp")
    }

    /**
     * 下载网络图片为 InputStream（调用方需在 IO 线程）
     */
    fun downloadImage(url: String): InputStream {
        Log.d(TAG, "downloadImage: $url")
        val request = Request.Builder().url(url)
            .header("User-Agent", UA)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("图片下载失败: HTTP ${response.code}")
        }
        return response.body?.byteStream()
            ?: throw IllegalStateException("图片下载失败: 响应体为空")
    }

    /**
     * 从 URI 获取 InputStream（支持本地 content:// 和网络 https://）
     * 调用方需在 IO 线程
     */
    fun openInputStream(uri: Uri): InputStream {
        return if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开文件: ${uri.lastPathSegment}")
        } else {
            downloadImage(uri.toString())
        }
    }
}
