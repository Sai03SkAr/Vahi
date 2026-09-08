package com.vahi.app.ui.components.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating vertical tool rail for the drawing canvas.
 * Wires directly to [CanvasState] — pen/eraser selection, a small colour palette,
 * stroke-width presets, and undo/redo/clear all already existed in CanvasState
 * with no UI to reach them (docs/09, "implemented but unreachable").
 *
 * Placement follows docs/12 §3.1: a left-edge rail keeps controls away from the
 * writing hand's palm on a landscape tablet. Handedness (mirroring to the right
 * edge) is left for a future pass — see docs/12 §3.1 and §8.1.
 */
@Composable
fun DrawingToolbar(
    canvasState: CanvasState,
    modifier: Modifier = Modifier
) {
    val palette = remember {
        listOf(
            Color(0xFF2A2A2A), // near-black ink
            Color(0xFF7C4DFF), // Vahi purple
            Color(0xFF1A73E8), // blue
            Color(0xFFD93025), // red
            Color(0xFF1E8E3E)  // green
        )
    }
    val widths = remember { listOf(2f, 4f, 8f) }

    Column(
        modifier = modifier
            .width(56.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarGlyphToggle(
            glyph = "✒", // ✒ pen
            selected = canvasState.activeTool == ToolType.PEN,
            onClick = { canvasState.activeTool = ToolType.PEN }
        )
        ToolbarGlyphToggle(
            glyph = "⌫", // ⌫ eraser
            selected = canvasState.activeTool == ToolType.ERASER,
            onClick = { canvasState.activeTool = ToolType.ERASER }
        )

        ToolbarDivider()

        palette.forEach { color ->
            ColorSwatch(
                color = color,
                selected = canvasState.activeTool == ToolType.PEN && canvasState.strokeColor == color,
                onClick = {
                    canvasState.strokeColor = color
                    canvasState.activeTool = ToolType.PEN
                }
            )
        }

        ToolbarDivider()

        widths.forEach { width ->
            WidthSwatch(
                width = width,
                selected = canvasState.strokeWidth == width,
                onClick = { canvasState.strokeWidth = width }
            )
        }

        ToolbarDivider()

        ToolbarGlyphButton(
            glyph = "↶", // ↶ undo
            enabled = canvasState.canUndo(),
            onClick = { canvasState.undo() }
        )
        ToolbarGlyphButton(
            glyph = "↷", // ↷ redo
            enabled = canvasState.canRedo(),
            onClick = { canvasState.redo() }
        )
        ToolbarIconButton(
            icon = Icons.Default.Delete,
            enabled = canvasState.strokes.isNotEmpty(),
            onClick = { canvasState.clearAll() }
        )
    }
}

@Composable
private fun ToolbarDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
        thickness = 1.dp,
        color = Color(0xFFECECEC)
    )
}

@Composable
private fun ToolbarGlyphToggle(
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFFE8E6FF) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            fontSize = 18.sp,
            color = if (selected) Color(0xFF7C4DFF) else Color(0xFF666666)
        )
    }
}

@Composable
private fun ToolbarGlyphButton(
    glyph: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            fontSize = 20.sp,
            color = if (enabled) Color(0xFF333333) else Color(0xFFCCCCCC)
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Clear canvas",
            tint = if (enabled) Color(0xFF333333) else Color(0xFFCCCCCC),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 28.dp else 22.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(2.dp, Color(0xFF7C4DFF), CircleShape)
                    else Modifier
                )
        )
    }
}

@Composable
private fun WidthSwatch(
    width: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFFE8E6FF) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = (width * 0.9f).dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF333333))
        )
    }
}
