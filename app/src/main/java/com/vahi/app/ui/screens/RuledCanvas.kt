package com.vahi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Ruled canvas content - vertical lines spanning full height.
 * Mimics a lined notebook page with vertical ruling.
 */
@Composable
fun RuledCanvasContent() {
    val density = LocalDensity.current
    val lineSpacingDp = 32.dp // Doubled spacing - half the number of lines
    val lineColor = Color(0xFFD0D8E0) // Light gray-blue color for all lines

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacingPx = with(density) { lineSpacingDp.toPx() }
            val lineCount = (size.width / lineSpacingPx).toInt()

            // Draw vertical lines from top to bottom
            for (i in 1..lineCount) {
                val x = i * lineSpacingPx
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
        }
    }
}
