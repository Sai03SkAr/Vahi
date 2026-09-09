package com.vahi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.ui.feature.theme.AppThemeViewModel
import com.vahi.app.ui.screens.AppRoot
import com.vahi.app.ui.theme.VahiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Vahi app.
 * This activity only hosts the Compose UI via setContent.
 * All UI logic is delegated to AppRoot.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() — see the SplashScreen API contract.
        // Dismisses automatically once the first frame draws; no video, no forced wait.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The theme preference has to be read here, above VahiTheme, since
            // VahiTheme is what wraps AppRoot (VAHI-307 → VAHI-406).
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val appTheme by themeViewModel.appTheme.collectAsState()

            VahiTheme(
                darkTheme = when (appTheme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                AppRoot()
            }
        }
    }
}
