package com.vahi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Window size classification for responsive layouts.
 * Based on Material Design breakpoints for adaptive UI.
 */
enum class WindowClass {
    /** Phone portrait: < 600dp */
    Compact,
    /** Phone landscape / small tablet: 600-839dp */
    Medium,
    /** Tablet / desktop: >= 840dp */
    Expanded
}

/**
 * Returns the current window size classification based on screen width.
 * Use this to implement responsive layouts that adapt between phone and tablet.
 *
 * Breakpoints:
 * - Compact: < 600dp (phones in portrait)
 * - Medium: 600-839dp (phones in landscape, small tablets)
 * - Expanded: >= 840dp (tablets, desktop)
 */
@Composable
fun windowClass(): WindowClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    return when {
        screenWidthDp < 600 -> WindowClass.Compact
        screenWidthDp < 840 -> WindowClass.Medium
        else -> WindowClass.Expanded
    }
}
