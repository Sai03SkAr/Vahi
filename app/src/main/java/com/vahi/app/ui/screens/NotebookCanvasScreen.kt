package com.vahi.app.ui.screens

import androidx.compose.runtime.Composable

/**
 * Enum representing the type of notebook canvas.
 */
enum class NotebookType {
    BLANK,
    RULED,
    GRID
}

/**
 * Main notebook canvas screen that renders the appropriate canvas based on type.
 */
@Composable
fun NotebookCanvasScreen(
    notebookType: NotebookType,
    onBack: () -> Unit
) {
    val canvasType = when (notebookType) {
        NotebookType.BLANK -> CanvasType.BLANK
        NotebookType.RULED -> CanvasType.RULED
        NotebookType.GRID -> CanvasType.GRID
    }

    CanvasScreen(
        canvasType = canvasType,
        onBack = onBack
    )
}
