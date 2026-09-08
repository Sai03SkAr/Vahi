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
                // Whether an erase gesture (pointer down with the eraser active) is
                // currently in progress. Local to this pointerInput block, which is
                // re-created whenever activeTool changes, so it starts fresh every
                // time the eraser is (re)selected — mirrors how currentStroke == null
                // signals "no pen gesture in progress" for the PEN branch below.
                var eraseGestureActive = false

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
                                    if (!eraseGestureActive) {
                                        // Pointer DOWN - begin batching this drag into one undo step
                                        canvasState.beginErase()
                                        eraseGestureActive = true
                                    }
                                    canvasState.eraseAtPosition(change.position)
                                    change.consume()
                                } else if (eraseGestureActive) {
                                    // Pointer UP - flush the batch (docs/09-project-status.md bug #5)
                                    canvasState.finishErase()
                                    eraseGestureActive = false
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
