package com.vahi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Grid (brainstorm) canvas - dotted grid with bidirectional scrolling.
 */
@Composable
fun GridCanvas() {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val dotSpacing = 24f // pixels between dots
    val dotRadius = 2f
    val dotColor = Color(0xFFD0D0D0) // Subtle gray
    val canvasSize = 4000.dp // Large canvas for infinite feel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        ) {
            // Canvas with dotted grid
            Canvas(
                modifier = Modifier.size(canvasSize)
            ) {
                val columns = (size.width / dotSpacing).toInt()
                val rows = (size.height / dotSpacing).toInt()

                for (row in 0..rows) {
                    for (col in 0..columns) {
                        val x = col * dotSpacing
                        val y = row * dotSpacing
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Temporary center text
            Box(
                modifier = Modifier.size(canvasSize),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Grid Canvas",
                    fontSize = 16.sp,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}
