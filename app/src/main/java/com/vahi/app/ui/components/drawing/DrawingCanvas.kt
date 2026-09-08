package com.vahi.app.ui.components.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A reusable, fullscreen drawing canvas composable.
 * 
 * Features:
 * - Transparent background (draws on top of existing content)
 * - Smooth freehand drawing with PEN tool
 * - Stroke-based erasing with ERASER tool
 * - Anti-aliased rendering with round caps and joins
 * 
 * @param canvasState The state object managing strokes and tool selection
 * @param modifier Additional modifiers to apply
 */
@Composable
fun DrawingCanvas(
    canvasState: CanvasState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(canvasState.activeTool) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        
                        when (canvasState.activeTool) {
                            ToolType.PEN -> {
                                if (change.pressed) {
                                    if (canvasState.currentStroke == null) {
                                        // Pointer DOWN - start new stroke
                                        canvasState.startStroke(change.position)
                                    } else {
                                        // Pointer MOVE - continue stroke
                                        canvasState.continueStroke(change.position)
                                    }
                                    change.consume()
                                } else {
                                    // Pointer UP - finish stroke
                                    if (canvasState.currentStroke != null) {
                                        canvasState.finishStroke()
                                    }
                                }
                            }
                            ToolType.ERASER -> {
                                if (change.pressed) {
                                    // Erase strokes at touch position
                                    canvasState.eraseAtPosition(change.position)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Draw all completed strokes
        canvasState.strokes.forEach { stroke ->
            drawPath(
                path = stroke.path,
                color = stroke.color,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Draw the current active stroke (if any)
        canvasState.currentStroke?.let { stroke ->
            drawPath(
                path = stroke.path,
                color = stroke.color,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
