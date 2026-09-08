package com.vahi.app.ui.components.drawing

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the command-history rewrite of undo/redo/erase (VAHI-007/120), fixing
 * docs/09-project-status.md bug #5: "Undo and erase share one stack." Before this
 * fix, erased strokes were pushed onto the *same* list used for stroke-add redo, so
 * redo() would un-erase a stroke instead of re-adding whatever had actually just
 * been undone, and finishStroke() clearing that list made drawing after an erase
 * silently permanent. Also covers batching a whole erase drag into one undo step
 * (previously one step per pointer sample).
 */
@RunWith(RobolectricTestRunner::class)
class CanvasStateUndoRedoTest {

    private fun CanvasState.drawOneStroke(x: Float = 0f, y: Float = 0f) {
        startStroke(Offset(x, y))
        continueStroke(Offset(x + 10f, y + 10f))
        finishStroke()
    }

    @Test
    fun `undoing a stroke addition removes exactly that stroke`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.drawOneStroke(50f, 50f)
        val secondStrokeId = state.strokes.last().id

        state.undo()

        assertEquals(1, state.strokes.size)
        assertFalse(state.strokes.any { it.id == secondStrokeId })
    }

    @Test
    fun `redo after undoing a stroke addition restores the same stroke`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        val strokeId = state.strokes.first().id

        state.undo()
        assertTrue(state.strokes.isEmpty())

        state.redo()
        assertEquals(1, state.strokes.size)
        assertEquals(strokeId, state.strokes.first().id)
    }

    @Test
    fun `redo never un-erases a stroke — it only replays the most recently undone action`() {
        // This is the exact scenario bug #5 got wrong: erase, then redo, must NOT
        // bring the erased stroke back, because erasing was never undone.
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)

        state.beginErase()
        state.eraseAtPosition(Offset(5f, 5f))
        state.finishErase()
        assertTrue("the erase should have actually removed the stroke", state.strokes.isEmpty())

        state.redo()

        assertTrue(
            "redo() with nothing undone must be a no-op — it must not un-erase",
            state.strokes.isEmpty()
        )
    }

    @Test
    fun `undoing an erase restores the erased stroke, and redo erases it again`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        val strokeId = state.strokes.first().id

        state.beginErase()
        state.eraseAtPosition(Offset(5f, 5f))
        state.finishErase()
        assertTrue(state.strokes.isEmpty())

        state.undo()
        assertEquals(1, state.strokes.size)
        assertEquals(strokeId, state.strokes.first().id)

        state.redo()
        assertTrue(state.strokes.isEmpty())
    }

    @Test
    fun `an entire erase drag across multiple strokes is a single undo step`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.drawOneStroke(20f, 20f)
        state.drawOneStroke(40f, 40f)
        assertEquals(3, state.strokes.size)

        // One drag, multiple samples along the way — as DrawingCanvas actually does it.
        state.beginErase()
        state.eraseAtPosition(Offset(5f, 5f))
        state.eraseAtPosition(Offset(25f, 25f))
        state.eraseAtPosition(Offset(45f, 45f))
        state.finishErase()
        assertTrue(state.strokes.isEmpty())

        // One undo brings back all three strokes erased in that single drag —
        // not one stroke per undo() call.
        state.undo()
        assertEquals(3, state.strokes.size)

        // Prove it really was ONE combined step, not three separate per-sample
        // erase commands that happened to net out to the same stroke count: the
        // *next* undo must revert the drawing of stroke 3 alone (one stroke gone),
        // not re-trigger any part of the erase (which would remove all three again).
        state.undo()
        assertEquals(
            "the next undo() should be undoing a single stroke *addition*, not the erase again",
            2, state.strokes.size
        )
    }

    @Test
    fun `drawing after undoing an erase does not resurrect it on a later redo`() {
        // The other half of bug #5: finishStroke() must clear the redo stack, so a
        // stale "redo" can't reach back past new ink to an old erase.
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)

        state.beginErase()
        state.eraseAtPosition(Offset(5f, 5f))
        state.finishErase()
        assertTrue(state.strokes.isEmpty())

        state.drawOneStroke(100f, 100f)
        assertEquals(1, state.strokes.size)

        assertFalse("the erase is no longer reachable via redo once new ink exists", state.canRedo())
    }

    @Test
    fun `an erase drag that touches no ink is not pushed as an undo step`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)

        state.beginErase()
        state.eraseAtPosition(Offset(9000f, 9000f)) // nowhere near the stroke
        state.finishErase()

        assertEquals(1, state.strokes.size)

        // Only the original draw should be on the undo stack — the empty erase
        // must not have pushed a second, no-op entry. One undo is enough to reach
        // an empty canvas.
        state.undo()
        assertTrue(state.strokes.isEmpty())
        assertFalse(
            "the empty erase gesture must not have consumed a second undo slot",
            state.canUndo()
        )
    }

    @Test
    fun `canUndo and canRedo reflect the command stacks, not just stroke count`() {
        val state = CanvasState()
        assertFalse(state.canUndo())
        assertFalse(state.canRedo())

        state.drawOneStroke()
        assertTrue(state.canUndo())
        assertFalse(state.canRedo())

        state.undo()
        assertFalse(state.canUndo())
        assertTrue(state.canRedo())
    }

    @Test
    fun `clearAll wipes the undo and redo history along with the strokes`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.drawOneStroke(50f, 50f)
        state.undo() // leaves one redo-able action on the stack

        state.clearAll()

        assertFalse(
            "undoing back through pre-clear strokes isn't offered — undoClear() is the way back",
            state.canUndo()
        )
        assertFalse(state.canRedo())
        assertTrue(state.canUndoClear())
    }

    @Test
    fun `undo is a no-op with an empty history`() {
        val state = CanvasState()
        state.undo() // must not throw
        assertTrue(state.strokes.isEmpty())
    }

    @Test
    fun `redo is a no-op with an empty history`() {
        val state = CanvasState()
        state.redo() // must not throw
        assertTrue(state.strokes.isEmpty())
    }
}
