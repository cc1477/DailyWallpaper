# DailyWallpaper 每日壁纸

基于 Material Design 3 的 Android 每日壁纸应用，支持多图源、定时更换、高斯模糊。

## 功能

### 图源
- **Bing 每日壁纸**：`https://www.bing.com/HPImageArchive.aspx`，支持 UHD / 1080p / 竖屏三种分辨率
- **栗次元 API**：`https://t.alcy.cc/json`，竖图/横图多分类可选，文档见 [t.alcy.cc/docs.html](https://t.alcy.cc/docs.html)
- **本地文件夹**：通过 SAF 授权选择目录，随机/顺序轮换，支持子目录
- **多源策略**：同时启用多个图源，调整优先级，失败自动降级

### 壁纸更换
- 桌面 / 锁屏可独立或同时更换
- 手动一键更换 + 定时自动更换（每日/每2日/每周）
- 失败自动重试，多图源自动降级兜底

### 高斯模糊
- 桌面与锁屏模糊强度分别独立调节（0~100%）

### 壁纸历史
- 缩略图网格展示，点击设为壁纸或保存相册
- 支持单条删除、清空全部、相同图片去重

### 相册保存
- 可选 .nomedia 标记隐藏

## 技术栈

Kotlin · Jetpack Compose · Material 3 · MVVM · Coil · OkHttp · DataStore · WorkManager

## 构建

```bash
./gradlew assembleDebug
```

minSdk 26 · targetSdk 34 · compileSdk 36 · JDK 17
