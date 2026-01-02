package com.vahi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vahi.app.ui.screens.AppRoot
import com.vahi.app.ui.theme.VahiTheme

/**
 * Main entry point for the Vahi app.
 * This activity only hosts the Compose UI via setContent.
 * All UI logic is delegated to AppRoot.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VahiTheme {
                AppRoot()
            }
        }
    }
}
