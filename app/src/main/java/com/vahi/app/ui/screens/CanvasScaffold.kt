package com.vahi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.ui.components.drawing.CanvasState
import com.vahi.app.ui.components.drawing.DrawingCanvas
import com.vahi.app.ui.components.drawing.DrawingToolbar
import kotlinx.coroutines.launch

/**
 * Unified fullscreen canvas screen.
 * Supports Blank, Ruled, and Grid paper styles.
 * Includes drawing layer with PEN and ERASER tools.
 *
 * @param notebookId the notebook this canvas belongs to. Not yet used by anything
 *   inside this composable — ink itself still isn't persisted, and CanvasState is
 *   still `remember`-scoped (docs/09-project-status.md bug #1, half-fixed). Threaded
 *   through now, while the navigation route is already being touched for the
 *   Session 6 persistence work, so a later session doesn't have to modify this
 *   route a second time purely to add an ID that was always going to be needed.
 */
@Composable
fun CanvasScreen(
    notebookId: String,
    canvasType: PaperStyle,
    onBack: () -> Unit
) {
    // Canvas state for drawing (strokes, undo/redo, tool selection)
    val canvasState = remember { CanvasState() }

    // Handle system back button
    BackHandler {
        onBack()
    }

    // Clear is one tap and sits directly below Undo/Redo — it used to be
    // unrecoverable (docs/09-project-status.md bug #13). CanvasState.clearAll()
    // now snapshots the page first; this offers it back via a snackbar.
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val onClear: () -> Unit = {
        canvasState.clearAll()
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Page cleared",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                canvasState.undoClear()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Layer 1: Background canvas (fullscreen)
        when (canvasType) {
            PaperStyle.BLANK -> BlankCanvasContent()
            PaperStyle.RULED -> RuledCanvasContent()
            PaperStyle.GRID -> GridCanvasContent()
        }

        // Layer 2: Drawing layer (transparent, handles drawing/erasing)
        DrawingCanvas(
            canvasState = canvasState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
        )

        // Layer 3: Floating tool rail — activates pen/eraser/colour/width/undo/redo/clear,
        // all of which existed in CanvasState with no way to reach them (docs/09).
        DrawingToolbar(
            canvasState = canvasState,
            onClear = onClear,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 16.dp)
                .zIndex(90f)
        )

        // Layer 4: Back arrow overlay
        // Position: Bottom-left for Blank and Ruled, Top-left for Grid
        val arrowAlignment = if (canvasType == PaperStyle.GRID) {
            Alignment.TopStart
        } else {
            Alignment.BottomStart
        }

        BackArrowButton(
            onClick = onBack,
            isGrid = canvasType == PaperStyle.GRID,
            modifier = Modifier
                .align(arrowAlignment)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(if (canvasType == PaperStyle.GRID) 16.dp else 24.dp)
                .zIndex(100f)
        )

        // Layer 5: Snackbar host — surfaces the "Undo" action after Clear.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 16.dp)
                .zIndex(110f)
        )
    }
}

/**
 * Back arrow button component.
 * Grid: Left arrow with circle background at top-left
 * Blank/Ruled: Down arrow without background at bottom-left
 */
@Composable
private fun BackArrowButton(
    onClick: () -> Unit,
    isGrid: Boolean,
    modifier: Modifier = Modifier
) {
    // Both variants carry an explicit contentDescription: this is the only way out of
    // the editor, and it was previously a bare glyph in a clickable Box — a screen
    // reader announced "left arrow" or nothing at all. Both are also sized to the
    // 48dp minimum (the grid variant was 44dp; the other had no minimum at all).
    if (isGrid) {
        // Grid: Original style with circle background, left arrow
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .clickable { onClick() }
                .semantics { contentDescription = "Back to library" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
    } else {
        // Blank/Ruled: Down arrow without background
        Box(
            modifier = modifier
                .size(48.dp)
                .clickable { onClick() }
                .semantics { contentDescription = "Back to library" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↓",
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333)
            )
        }
    }
}
