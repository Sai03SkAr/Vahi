package com.vahi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Blank canvas content - pure white fullscreen background.
 * No text, no watermarks, full screen area.
 */
@Composable
fun BlankCanvasContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
