package com.daily.wallpaper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object History : Screen("history")
    data object Sources : Screen("sources")
    data object Schedule : Screen("schedule")
    data object Effects : Screen("effects")
    data object Save : Screen("save")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)
