package com.daily.wallpaper.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daily.wallpaper.data.model.ChangeFrequency
import com.daily.wallpaper.ui.MainViewModel
import com.daily.wallpaper.ui.components.SectionHeader
import com.daily.wallpaper.ui.components.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsState()

    val timePickerState = rememberTimePickerState(
        initialHour = settings.changeHour,
        initialMinute = settings.changeMinute,
        is24Hour = true,
    )

    androidx.compose.runtime.LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        if (timePickerState.hour != settings.changeHour || timePickerState.minute != settings.changeMinute) {
            viewModel.updateSettings {
                it.copy(changeHour = timePickerState.hour, changeMinute = timePickerState.minute)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时设置", fontWeight = FontWeight.Bold) },
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
            // ── 自动更换开关 ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    SwitchPreferenceItem(
                        title = "自动更换壁纸",
                        subtitle = if (settings.autoChangeEnabled) "已开启" else "已关闭",
                        icon = Icons.Filled.Schedule,
                        checked = settings.autoChangeEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(autoChangeEnabled = enabled) }
                            if (enabled) {
                                viewModel.scheduleAutoChange()
                            } else {
                                viewModel.cancelAutoChange()
                            }
                        },
                    )
                }
            }

            // ── 时间选择器 ──
            if (settings.autoChangeEnabled) {
                item { SectionHeader("更换时间") }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    selectorColor = MaterialTheme.colorScheme.primary,
                                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }

                // ── 更换频率 ──
                item { SectionHeader("更换频率") }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                ChangeFrequency.entries.forEachIndexed { index, freq ->
                                    SegmentedButton(
                                        selected = settings.changeFrequency == freq,
                                        onClick = {
                                            viewModel.updateSettings { it.copy(changeFrequency = freq) }
                                            viewModel.scheduleAutoChange()
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ChangeFrequency.entries.size,
                                        ),
                                    ) {
                                        Text(freq.displayName)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 暂停/恢复 ──
                item { SectionHeader("操作") }
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
                        Column(modifier = Modifier.padding(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.updateSettings { it.copy(autoChangeEnabled = false) }
                                    viewModel.cancelAutoChange()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("暂停自动更换")
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.changeWallpaperNow()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("立即执行一次")
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "下次自动更换: 每${settings.changeFrequency.days}天 ${String.format("%02d:%02d", settings.changeHour, settings.changeMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
    }
}
