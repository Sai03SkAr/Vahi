package com.vahi.app.ui.components.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.UUID

/**
 * Represents a single drawing stroke on the canvas.
 *
 * @param id Unique identifier for undo/redo operations
 * @param path The graphics path containing stroke points
 * @param color The color of the stroke
 * @param strokeWidth The width/thickness of the stroke
 */
data class Stroke(
    val id: UUID = UUID.randomUUID(),
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f
)
