package com.vahi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Grid canvas content - dotted grid with bidirectional scrolling.
 * Unchanged grid visuals.
 */
@Composable
fun GridCanvasContent() {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val dotSpacing = 24f
    val dotRadius = 2f
    val dotColor = Color(0xFFD0D0D0)
    val canvasSize = 4000.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(verticalScrollState)
            .horizontalScroll(horizontalScrollState)
    ) {
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
    }
}
