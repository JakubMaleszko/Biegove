package com.jakubmaleszko.biegove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakubmaleszko.biegove.ui.theme.BiegoveTheme
import com.tencent.mmkv.MMKV

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        MMKV.initialize(this)

        setContent {
            val viewModel: BiegoveViewModel = viewModel()
            val settings by viewModel.settingsState.collectAsState()
                val darkTheme = when (settings?.themeMode) {
                    1 -> false // Light
                    2 -> true  // Dark
                    else -> isSystemInDarkTheme() // System (0 or null)
                }
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme }
                )
                BiegoveTheme(darkTheme) {
                    NavManager(viewModel)
                }
        }
    }
}