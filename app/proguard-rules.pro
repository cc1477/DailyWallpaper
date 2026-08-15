# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.daily.wallpaper.data.model.** { *; }
-keep class com.google.gson.** { *; }

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ── Coil ──
-dontwarn coil.**

# ── Compose ──
-dontwarn androidx.compose.**

# ── 保留堆栈信息 ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
