package com.daily.wallpaper.ui.save

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daily.wallpaper.data.model.HideStrategy
import com.daily.wallpaper.ui.MainViewModel
import com.daily.wallpaper.ui.components.SectionHeader
import com.daily.wallpaper.ui.components.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("保存设置", fontWeight = FontWeight.Bold) },
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
            // ── 保存到相册开关 ──
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    SwitchPreferenceItem(
                        title = "自动保存到相册",
                        subtitle = "更换壁纸时自动保存原图到系统相册",
                        icon = Icons.Filled.PhotoLibrary,
                        checked = settings.saveToAlbum,
                        onCheckedChange = { value ->
                            viewModel.updateSettings { it.copy(saveToAlbum = value) }
                        },
                    )
                }
            }

            if (settings.saveToAlbum) {
                // ── 隐藏策略 ──
                item { SectionHeader("相册隐藏策略") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column {
                            HideStrategy.entries.forEach { strategy ->
                                RadioRow(
                                    title = strategy.displayName,
                                    subtitle = getStrategyDescription(strategy),
                                    icon = getStrategyIcon(strategy),
                                    selected = settings.hideStrategy == strategy,
                                    onSelect = {
                                        viewModel.updateSettings { it.copy(hideStrategy = strategy) }
                                    },
                                )
                                if (strategy != HideStrategy.entries.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                                }
                            }
                        }
                    }
                }

                // ── 说明 ──
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "💡 说明",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• 不隐藏：壁纸正常出现在相册最新位置\n" +
                                    "• .nomedia 标记：相册完全隐藏该目录，需要时用文件管理器查看",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RadioRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

private fun getStrategyDescription(strategy: HideStrategy): String = when (strategy) {
    HideStrategy.NONE -> "壁纸正常出现在相册最新位置"
    HideStrategy.NOMEDIA -> "相册完全隐藏该目录，需要时用文件管理器查看"
}

private fun getStrategyIcon(strategy: HideStrategy): ImageVector = when (strategy) {
    HideStrategy.NONE -> Icons.Filled.PhotoLibrary
    HideStrategy.NOMEDIA -> Icons.Filled.FolderOff
}
