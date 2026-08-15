package com.daily.wallpaper.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: com.daily.wallpaper.ui.MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val wallpaperState by viewModel.wallpaperState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日壁纸", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "历史")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 当前壁纸预览（从顶部裁剪，底部高斯模糊过渡） ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentImage = wallpaperState.currentWallpaper?.imageUrl
                        ?: wallpaperState.history.firstOrNull()?.imageUrl

                    if (currentImage != null) {
                        // 底层：清晰图片，从顶部对齐裁剪
                        AsyncImage(
                            model = currentImage,
                            contentDescription = "当前壁纸",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopStart,
                        )

                        // 模糊层：同一张图轻模糊后叠在上面，用 DstIn 渐变让底部渐显
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    // DstIn: 透明区域擦除模糊露出清晰图，不透明区域保留模糊
                                    // 渐变范围拉长让过渡更柔和，只在底部 1/4 渐显
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.72f to Color.Transparent,
                                            1f to Color.Black,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                },
                        ) {
                            AsyncImage(
                                model = currentImage,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(15.dp),
                                contentScale = ContentScale.FillWidth,
                                alignment = Alignment.TopStart,
                            )
                        }

                        // 底部轻微暗化，让文字更清晰
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.7f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.35f),
                                    )
                                ),
                        )
                        // 壁纸信息
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val wp = wallpaperState.currentWallpaper
                                ?: wallpaperState.history.firstOrNull()
                            Text(
                                text = wp?.title ?: "当前壁纸",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            if (!wp?.copyright.isNullOrEmpty()) {
                                Text(
                                    text = wp!!.copyright!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "点击下方按钮获取壁纸",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (wallpaperState.isChanging) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // ── 立即更换按钮（紧贴预览图下方） ──
            Button(
                onClick = { viewModel.changeWallpaperNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !wallpaperState.isChanging && (settings.targetHome || settings.targetLock),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("立即更换壁纸", style = MaterialTheme.typography.titleMedium)
            }

            // ── 更换目标选择 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilterChip(
                    selected = settings.targetHome,
                    onClick = { viewModel.updateSettings { it.copy(targetHome = !it.targetHome) } },
                    label = { Text("桌面壁纸", style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        Icon(Icons.Filled.DesktopWindows, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                )
                FilterChip(
                    selected = settings.targetLock,
                    onClick = { viewModel.updateSettings { it.copy(targetLock = !it.targetLock) } },
                    label = { Text("锁屏壁纸", style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                )
            }

            // ── 保存到相册 ──
            FilledTonalButton(
                onClick = { viewModel.saveCurrentToAlbum() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("保存当前壁纸到相册")
            }

            // ── 图源信息卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "已启用图源",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (settings.enabledSources.isEmpty()) {
                                "未启用"
                            } else {
                                settings.enabledSources.joinToString(" · ") { it.displayName }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = "${settings.enabledSources.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
