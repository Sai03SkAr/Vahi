package com.vahi.app.ui.components.drawing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.UUID

/**
 * Tool types available for canvas interaction.
 */
enum class ToolType {
    PEN,
    ERASER
}

/**
 * Manages the state of the drawing canvas including strokes, undo/redo stacks,
 * and active tool selection.
 */
class CanvasState {
    
    /** List of completed strokes on the canvas */
    private val _strokes = mutableStateListOf<Stroke>()
    val strokes: List<Stroke> get() = _strokes
    
    /** Stack of undone strokes for redo functionality */
    private val _undoneStrokes = mutableStateListOf<Stroke>()
    val undoneStrokes: List<Stroke> get() = _undoneStrokes
    
    /** Currently active stroke being drawn (null when not drawing) */
    var currentStroke: Stroke? by mutableStateOf(null)
        private set
    
    /**
     * Snapshot of [_strokes] taken immediately before the most recent [clearAll], so a
     * mis-tap on Clear can be undone once. Dropped the moment any other mutating action
     * happens (a new stroke, an erase, another clear) — restoring an older snapshot after
     * that would silently discard whatever the user drew in between.
     */
    private var lastClearedSnapshot: List<Stroke>? = null

    /** Currently selected tool */
    var activeTool: ToolType by mutableStateOf(ToolType.PEN)
    
    /** Default stroke color */
    var strokeColor: Color by mutableStateOf(Color.Black)
    
    /** Default stroke width */
    var strokeWidth: Float by mutableStateOf(4f)
    
    /**
     * Starts a new stroke at the given position.
     */
    fun startStroke(position: Offset) {
        val path = Path().apply {
            moveTo(position.x, position.y)
        }
        currentStroke = Stroke(
            id = UUID.randomUUID(),
            path = path,
            color = strokeColor,
            strokeWidth = strokeWidth
        )
    }
    
    /**
     * Continues the current stroke to the given position.
     */
    fun continueStroke(position: Offset) {
        currentStroke?.let { stroke ->
            stroke.path.lineTo(position.x, position.y)
            // Force recomposition by creating new reference
            currentStroke = stroke.copy(path = stroke.path)
        }
    }
    
    /**
     * Finishes the current stroke and adds it to the strokes list.
     */
    fun finishStroke() {
        currentStroke?.let { stroke ->
            _strokes.add(stroke)
            // Clear redo stack when new stroke is added
            _undoneStrokes.clear()
            // A clear can no longer be undone once new ink exists on top of it.
            lastClearedSnapshot = null
        }
        currentStroke = null
    }
    
    /**
     * Cancels the current stroke without adding it to the list.
     */
    fun cancelStroke() {
        currentStroke = null
    }
    
    /**
     * Erases strokes that intersect with the given position.
     * Uses a hit radius for touch tolerance.
     */
    fun eraseAtPosition(position: Offset, hitRadius: Float = 20f) {
        val hitRect = Rect(
            left = position.x - hitRadius,
            top = position.y - hitRadius,
            right = position.x + hitRadius,
            bottom = position.y + hitRadius
        )
        
        val strokesToRemove = _strokes.filter { stroke ->
            strokeIntersectsRect(stroke, hitRect)
        }
        
        if (strokesToRemove.isNotEmpty()) {
            // A clear can no longer be undone once the page has changed again.
            lastClearedSnapshot = null
        }
        strokesToRemove.forEach { stroke ->
            _strokes.remove(stroke)
            _undoneStrokes.add(stroke)
        }
    }
    
    /**
     * Checks if a stroke intersects with the given rectangle.
     * Uses path bounds for simple hit detection.
     */
    private fun strokeIntersectsRect(stroke: Stroke, rect: Rect): Boolean {
        val pathBounds = stroke.path.getBounds()
        return pathBounds.overlaps(rect)
    }
    
    /**
     * Undoes the last stroke action.
     */
    fun undo() {
        if (_strokes.isNotEmpty()) {
            val lastStroke = _strokes.removeLast()
            _undoneStrokes.add(lastStroke)
        }
    }
    
    /**
     * Redoes the last undone stroke action.
     */
    fun redo() {
        if (_undoneStrokes.isNotEmpty()) {
            val lastUndone = _undoneStrokes.removeLast()
            _strokes.add(lastUndone)
        }
    }
    
    /**
     * Checks if undo is available.
     */
    fun canUndo(): Boolean = _strokes.isNotEmpty()
    
    /**
     * Checks if redo is available.
     */
    fun canRedo(): Boolean = _undoneStrokes.isNotEmpty()
    
    /**
     * Clears all strokes from the canvas.
     *
     * Takes a snapshot first so [undoClear] can restore the page. Previously this emptied
     * the redo stack too, making a mis-tap on the toolbar's Clear button — a 48dp target
     * sitting directly below Undo/Redo — unrecoverable data loss (docs/09-project-status.md
     * bug #13). No-op if the page is already empty, so an empty clear can't overwrite a
     * still-restorable earlier snapshot.
     */
    fun clearAll() {
        if (_strokes.isEmpty()) return
        lastClearedSnapshot = _strokes.toList()
        _strokes.clear()
        _undoneStrokes.clear()
        currentStroke = null
    }

    /** Whether the most recent [clearAll] can still be undone. */
    fun canUndoClear(): Boolean = lastClearedSnapshot != null

    /**
     * Restores the page to how it was immediately before the most recent [clearAll].
     * Only available until another mutating action happens — see [lastClearedSnapshot].
     */
    fun undoClear() {
        lastClearedSnapshot?.let { snapshot ->
            _strokes.clear()
            _strokes.addAll(snapshot)
            _undoneStrokes.clear()
            lastClearedSnapshot = null
        }
    }
}
