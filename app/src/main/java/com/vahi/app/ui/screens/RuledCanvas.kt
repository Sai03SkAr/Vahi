package com.vahi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ruled canvas - VERTICAL lines spanning the entire canvas height.
 */
@Composable
fun RuledCanvas() {
    val scrollState = rememberScrollState()
    val lineSpacing = 40f // pixels between vertical lines
    val lineColor = Color(0x40B8D4E8) // Subtle blue with low opacity
    val canvasHeight = 4000.dp // Simulates infinite scroll

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                val lineCount = (size.width / lineSpacing).toInt()

                // Draw VERTICAL lines
                for (i in 0..lineCount) {
                    val x = i * lineSpacing
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
}
