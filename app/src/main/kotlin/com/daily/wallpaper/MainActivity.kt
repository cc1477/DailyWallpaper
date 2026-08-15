package com.daily.wallpaper

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daily.wallpaper.ui.MainViewModel
import com.daily.wallpaper.ui.navigation.AppNavigation
import com.daily.wallpaper.ui.theme.DailyWallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val darkMode = isSystemInDarkTheme()

            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            DailyWallpaperTheme {
                val viewModel: MainViewModel = viewModel()
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
