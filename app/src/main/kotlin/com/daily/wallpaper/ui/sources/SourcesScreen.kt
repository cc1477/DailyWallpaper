package com.daily.wallpaper.ui.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daily.wallpaper.data.model.AlcyCategory
import com.daily.wallpaper.data.model.BingResolution
import com.daily.wallpaper.data.model.SourceType
import com.daily.wallpaper.ui.MainViewModel
import com.daily.wallpaper.ui.components.SectionHeader
import com.daily.wallpaper.ui.components.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsState()
    var localImageCount by remember { mutableStateOf(0) }

    // SAF 文件夹选择器
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.persistFolderPermission(uri)
            viewModel.setLocalFolderUri(uri)
        }
    }

    // 刷新本地图片数量
    androidx.compose.runtime.LaunchedEffect(settings.localFolderUri, settings.localIncludeSubdirs) {
        localImageCount = if (settings.localFolderUri != null) {
            viewModel.getLocalImageCount()
        } else {
            0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图源管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item { SectionHeader("图源列表（拖动箭头调整优先级）") }

            // ── Bing ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    SwitchPreferenceItem(
                        title = SourceType.BING.displayName,
                        subtitle = "每日自动更新，附带版权信息",
                        checked = settings.enabledSources.contains(SourceType.BING),
                        onCheckedChange = { viewModel.toggleSource(SourceType.BING) },
                    )
                    if (settings.enabledSources.contains(SourceType.BING)) {
                        SourcePriorityControls(
                            source = SourceType.BING,
                            sources = settings.enabledSources,
                            onMoveUp = { viewModel.moveSource(SourceType.BING, true) },
                            onMoveDown = { viewModel.moveSource(SourceType.BING, false) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                        BingResolutionSelector(
                            selectedResolution = settings.bingResolution,
                            onResolutionSelected = { res ->
                                viewModel.updateSettings { it.copy(bingResolution = res) }
                            },
                        )
                    }
                }
            }

            // ── 栗次元 ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    SwitchPreferenceItem(
                        title = SourceType.ALCY.displayName,
                        subtitle = "二次元 / 风景 / AI 等多分类",
                        checked = settings.enabledSources.contains(SourceType.ALCY),
                        onCheckedChange = { viewModel.toggleSource(SourceType.ALCY) },
                    )
                    if (settings.enabledSources.contains(SourceType.ALCY)) {
                        SourcePriorityControls(
                            source = SourceType.ALCY,
                            sources = settings.enabledSources,
                            onMoveUp = { viewModel.moveSource(SourceType.ALCY, true) },
                            onMoveDown = { viewModel.moveSource(SourceType.ALCY, false) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                        AlcyCategorySelector(
                            selectedCategory = settings.alcyCategory,
                            onCategorySelected = { category ->
                                viewModel.updateSettings { it.copy(alcyCategory = category) }
                            },
                        )
                    }
                }
            }

            // ── 本地文件夹 ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    SwitchPreferenceItem(
                        title = SourceType.LOCAL.displayName,
                        subtitle = "从本地文件夹随机/顺序轮换",
                        checked = settings.enabledSources.contains(SourceType.LOCAL),
                        onCheckedChange = { viewModel.toggleSource(SourceType.LOCAL) },
                    )
                    if (settings.enabledSources.contains(SourceType.LOCAL)) {
                        SourcePriorityControls(
                            source = SourceType.LOCAL,
                            sources = settings.enabledSources,
                            onMoveUp = { viewModel.moveSource(SourceType.LOCAL, true) },
                            onMoveDown = { viewModel.moveSource(SourceType.LOCAL, false) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                        LocalFolderConfig(
                            folderUri = settings.localFolderUri,
                            imageCount = localImageCount,
                            includeSubdirs = settings.localIncludeSubdirs,
                            sequentialMode = settings.localSequentialMode,
                            avoidRepeat = settings.localAvoidRepeat,
                            onPickFolder = { folderLauncher.launch(null) },
                            onRescan = { localImageCount = viewModel.getLocalImageCount() },
                            onIncludeSubdirsChange = { value ->
                                viewModel.updateSettings { it.copy(localIncludeSubdirs = value) }
                            },
                            onSequentialChange = { value ->
                                viewModel.updateSettings { it.copy(localSequentialMode = value) }
                            },
                            onAvoidRepeatChange = { value ->
                                viewModel.updateSettings { it.copy(localAvoidRepeat = value) }
                            },
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "提示：可同时启用多个图源，按优先级顺序获取，高优先级失败时自动降级到备选图源",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun SourcePriorityControls(
    source: SourceType,
    sources: List<SourceType>,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val index = sources.indexOf(source)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "优先级 #${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "上移")
        }
        IconButton(onClick = onMoveDown, enabled = index < sources.size - 1) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下移")
        }
    }
}

@Composable
private fun AlcyCategorySelector(
    selectedCategory: AlcyCategory,
    onCategorySelected: (AlcyCategory) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = "选择分类",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        // 竖图优先
        Text(
            text = "竖图（推荐）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val verticalCategories = AlcyCategory.entries.filter { it.isVertical }
        CategoryChipRow(
            categories = verticalCategories,
            selected = selectedCategory,
            onSelect = onCategorySelected,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "横图（备选）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val horizontalCategories = AlcyCategory.entries.filter { !it.isVertical }
        CategoryChipRow(
            categories = horizontalCategories,
            selected = selectedCategory,
            onSelect = onCategorySelected,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipRow(
    categories: List<AlcyCategory>,
    selected: AlcyCategory,
    onSelect: (AlcyCategory) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.displayName, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
private fun LocalFolderConfig(
    folderUri: String?,
    imageCount: Int,
    includeSubdirs: Boolean,
    sequentialMode: Boolean,
    avoidRepeat: Boolean,
    onPickFolder: () -> Unit,
    onRescan: () -> Unit,
    onIncludeSubdirsChange: (Boolean) -> Unit,
    onSequentialChange: (Boolean) -> Unit,
    onAvoidRepeatChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        // 选择文件夹按钮
        FilledTonalButton(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (folderUri != null) "已选择文件夹" else "选择壁纸文件夹")
        }

        // 文件夹信息
        if (folderUri != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "图片总数: $imageCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onRescan) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重新扫描")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SwitchPreferenceItem(
            title = "包含子目录",
            subtitle = "扫描所选文件夹及其子目录",
            checked = includeSubdirs,
            onCheckedChange = onIncludeSubdirsChange,
        )
        SwitchPreferenceItem(
            title = "顺序轮播",
            subtitle = if (sequentialMode) "按顺序依次更换" else "随机选取",
            checked = sequentialMode,
            onCheckedChange = onSequentialChange,
        )
        SwitchPreferenceItem(
            title = "避免连续重复",
            subtitle = "不连续选取同一张图片",
            checked = avoidRepeat,
            onCheckedChange = onAvoidRepeatChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BingResolutionSelector(
    selectedResolution: BingResolution,
    onResolutionSelected: (BingResolution) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = "Bing 分辨率",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "UHD 超清横屏裁为竖屏效果最佳；竖屏选项为 Bing 原生裁剪无超分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BingResolution.entries.forEach { res ->
                FilterChip(
                    selected = selectedResolution == res,
                    onClick = { onResolutionSelected(res) },
                    label = { Text(res.displayName, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}
