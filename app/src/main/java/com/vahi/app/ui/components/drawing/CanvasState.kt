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
 * One user-visible, undoable action. Deliberately narrower than the full command
 * model in docs/16-canvas-spec.md §9 (which assumes the future ink-engine types,
 * `FinishedStroke`/`PageState`, that don't exist until the androidx.ink decision (D6)
 * is settled by the latency benchmark) — this fixes the *current* model's real bug
 * without reaching ahead of that decision. `Clear` is deliberately not a case here;
 * see the note on [CanvasState.clearAll].
 */
private sealed interface StrokeCommand {
    data class Add(val stroke: Stroke) : StrokeCommand
    data class Erase(val strokes: List<Stroke>) : StrokeCommand
}

/**
 * Manages the state of the drawing canvas including strokes, undo/redo history,
 * and active tool selection.
 */
class CanvasState {

    /** List of completed strokes on the canvas */
    private val _strokes = mutableStateListOf<Stroke>()
    val strokes: List<Stroke> get() = _strokes

    /**
     * Command history for undo/redo. Replaces the previous design, where erased
     * strokes were pushed onto the *same* list used for stroke-add redo — so
     * `redo()` would un-erase a stroke instead of re-adding whatever had actually
     * just been undone, and `finishStroke()` clearing that list meant drawing
     * after an erase silently made the erase permanent with no way back
     * (docs/09-project-status.md bug #5). Each user action — one pen stroke, or
     * one whole erase drag — is exactly one entry here.
     */
    private val undoStack = mutableStateListOf<StrokeCommand>()
    private val redoStack = mutableStateListOf<StrokeCommand>()

    /** Strokes removed so far during an in-progress erase gesture, or null when
     *  no erase gesture is active. Accumulated across [eraseAtPosition] calls and
     *  flushed as a single [StrokeCommand.Erase] by [finishErase] — without this,
     *  a single erase drag crossing several strokes would push one undo step per
     *  pointer sample instead of one step for the whole gesture. */
    private var eraseSessionRemoved: MutableList<Stroke>? = null

    /** Currently active stroke being drawn (null when not drawing) */
    var currentStroke: Stroke? by mutableStateOf(null)
        private set

    /**
     * Snapshot of [_strokes] taken immediately before the most recent [clearAll], so a
     * mis-tap on Clear can be undone once. Dropped the moment any other mutating action
     * happens (a new stroke, an erase, another clear) — restoring an older snapshot after
     * that would silently discard whatever the user drew in between.
     *
     * Kept as its own single-slot mechanism rather than folded into [undoStack]: if Clear
     * were just another entry on that stack, the toolbar's generic Undo would undo
     * whichever action is *most recent* — not necessarily the clear a snackbar's "Undo"
     * button is specifically offering to reverse. A user who draws again while that
     * snackbar is still showing and then taps it would have their new stroke undone
     * instead of the page restored. One clear, one dedicated undo slot, sidesteps that.
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
     * Finishes the current stroke, adds it to the strokes list, and pushes an
     * undoable [StrokeCommand.Add].
     */
    fun finishStroke() {
        currentStroke?.let { stroke ->
            _strokes.add(stroke)
            pushCommand(StrokeCommand.Add(stroke))
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
     * Marks the start of an erase gesture (pointer down with the eraser tool
     * active). Strokes removed before the matching [finishErase] are batched into
     * one undo step. The caller (`DrawingCanvas`) is responsible for pairing every
     * `beginErase()` with exactly one `finishErase()` per gesture.
     */
    fun beginErase() {
        eraseSessionRemoved = mutableListOf()
    }

    /**
     * Erases strokes that intersect with the given position. Uses a hit radius for
     * touch tolerance. If called between [beginErase] and [finishErase] (the normal
     * path, from `DrawingCanvas`), removed strokes accumulate into that gesture's
     * single undo step rather than becoming their own. If called with no gesture in
     * progress, each call is its own undo step — kept as a safety net for direct
     * callers (tests, or any future non-gestural caller), not the expected path.
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
        if (strokesToRemove.isEmpty()) return

        strokesToRemove.forEach { _strokes.remove(it) }
        // A clear can no longer be undone once the page has changed again.
        lastClearedSnapshot = null

        val session = eraseSessionRemoved
        if (session != null) {
            session.addAll(strokesToRemove)
        } else {
            pushCommand(StrokeCommand.Erase(strokesToRemove))
        }
    }

    /**
     * Marks the end of an erase gesture (pointer up). Flushes everything removed
     * since [beginErase] as a single undoable step. No-op if nothing was erased
     * during the gesture (e.g. the user dragged the eraser without touching any ink).
     */
    fun finishErase() {
        val removed = eraseSessionRemoved
        eraseSessionRemoved = null
        if (!removed.isNullOrEmpty()) {
            pushCommand(StrokeCommand.Erase(removed))
        }
    }

    /** Pushes a completed action and clears the redo stack, per the usual undo/redo rule:
     *  a fresh action invalidates whatever "future" a prior undo had exposed. */
    private fun pushCommand(command: StrokeCommand) {
        undoStack.add(command)
        redoStack.clear()
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
     * Undoes the last action — one pen stroke, or one whole erase gesture.
     */
    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        when (command) {
            is StrokeCommand.Add -> _strokes.remove(command.stroke)
            is StrokeCommand.Erase -> _strokes.addAll(command.strokes)
        }
        redoStack.add(command)
    }

    /**
     * Redoes the last undone action.
     */
    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        when (command) {
            is StrokeCommand.Add -> _strokes.add(command.stroke)
            is StrokeCommand.Erase -> _strokes.removeAll(command.strokes)
        }
        undoStack.add(command)
    }

    /**
     * Checks if undo is available.
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Checks if redo is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Clears all strokes from the canvas.
     *
     * Takes a snapshot first so [undoClear] can restore the page. Previously this emptied
     * the redo stack too, making a mis-tap on the toolbar's Clear button — a 48dp target
     * sitting directly below Undo/Redo — unrecoverable data loss (docs/09-project-status.md
     * bug #13). No-op if the page is already empty, so an empty clear can't overwrite a
     * still-restorable earlier snapshot.
     *
     * Also clears the Add/Erase undo history: once the page is blank, undoing back through
     * individual strokes from before the clear isn't a coherent action to offer — restoring
     * everything at once via [undoClear] is the one clear (pun intended) way back.
     */
    fun clearAll() {
        if (_strokes.isEmpty()) return
        lastClearedSnapshot = _strokes.toList()
        _strokes.clear()
        undoStack.clear()
        redoStack.clear()
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
            undoStack.clear()
            redoStack.clear()
            lastClearedSnapshot = null
        }
    }
}
