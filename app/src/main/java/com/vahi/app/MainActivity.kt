package com.vahi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
            VahiTheme {
                AppRoot()
            }
        }
    }
}
