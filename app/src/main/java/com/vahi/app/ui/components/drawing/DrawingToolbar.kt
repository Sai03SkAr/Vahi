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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahi.app.ui.theme.VahiInkBlack
import com.vahi.app.ui.theme.VahiInkBlue
import com.vahi.app.ui.theme.VahiInkGreen
import com.vahi.app.ui.theme.VahiInkPurple
import com.vahi.app.ui.theme.VahiInkRed

/** One stroke-width preset: the actual canvas width it draws with, and an
 *  independently-chosen preview size for the swatch. These used to be the
 *  same Float reinterpreted as dp (docs/09-project-status.md bug #18) —
 *  keeping them as two explicit fields makes that unit mixup impossible. */
private data class WidthPreset(val strokeWidthPx: Float, val previewHeight: Dp)

/**
 * Floating vertical tool rail for the drawing canvas.
 * Wires directly to [CanvasState] — pen/eraser selection, a small colour palette,
 * stroke-width presets, and undo/redo all already existed in CanvasState
 * with no UI to reach them (docs/09, "implemented but unreachable").
 *
 * Placement follows docs/12 §3.1: a left-edge rail keeps controls away from the
 * writing hand's palm on a landscape tablet. Handedness (mirroring to the right
 * edge) is left for a future pass — see docs/12 §3.1 and §8.1.
 *
 * @param onClear invoked when the user taps Clear, instead of calling
 *   [CanvasState.clearAll] directly, so the caller can offer "Undo" (the button
 *   sits right below Undo/Redo, and a mis-tap here is the single most severe
 *   bug found in the Session 5 re-audit — docs/09-project-status.md bug #13).
 */
@Composable
fun DrawingToolbar(
    canvasState: CanvasState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember {
        listOf(VahiInkBlack, VahiInkPurple, VahiInkBlue, VahiInkRed, VahiInkGreen)
    }
    val widthPresets = remember {
        listOf(
            WidthPreset(strokeWidthPx = 2f, previewHeight = 3.dp),
            WidthPreset(strokeWidthPx = 4f, previewHeight = 6.dp),
            WidthPreset(strokeWidthPx = 8f, previewHeight = 11.dp)
        )
    }

    Column(
        modifier = modifier
            .width(56.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarGlyphToggle(
            glyph = "✒",
            label = "Pen tool",
            selected = canvasState.activeTool == ToolType.PEN,
            onClick = { canvasState.activeTool = ToolType.PEN }
        )
        ToolbarGlyphToggle(
            glyph = "⌫",
            label = "Eraser tool",
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

        widthPresets.forEach { preset ->
            WidthSwatch(
                preset = preset,
                selected = canvasState.strokeWidth == preset.strokeWidthPx,
                onClick = { canvasState.strokeWidth = preset.strokeWidthPx }
            )
        }

        ToolbarDivider()

        ToolbarGlyphButton(
            glyph = "↶",
            label = "Undo",
            enabled = canvasState.canUndo(),
            onClick = { canvasState.undo() }
        )
        ToolbarGlyphButton(
            glyph = "↷",
            label = "Redo",
            enabled = canvasState.canRedo(),
            onClick = { canvasState.redo() }
        )
        ToolbarIconButton(
            icon = Icons.Default.Delete,
            label = "Clear page",
            enabled = canvasState.strokes.isNotEmpty(),
            onClick = onClear
        )
    }
}

@Composable
private fun ToolbarDivider() {
    HorizontalDivider(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .clearAndSetSemantics {},
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * A 48dp selectable circular toggle. The clickable/selectable modifier is applied
 * directly to the full-size box — a prior version applied `.padding(4.dp)` before
 * `.clickable`, shrinking the actual touch target to 40dp (docs/09 bug #15).
 */
@Composable
private fun ToolbarGlyphToggle(
    glyph: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            fontSize = 18.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolbarGlyphButton(
    glyph: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            fontSize = 20.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // described by the parent Box's semantics above
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics { contentDescription = "Ink colour" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 28.dp else 22.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                )
        )
    }
}

@Composable
private fun WidthSwatch(
    preset: WidthPreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics { contentDescription = "Stroke width" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = preset.previewHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}
