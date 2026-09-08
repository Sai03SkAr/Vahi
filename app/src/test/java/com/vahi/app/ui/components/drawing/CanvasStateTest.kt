package com.vahi.app.ui.components.drawing

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers CanvasState.clearAll()/undoClear() — the fix for
 * docs/09-project-status.md bug #13 ("Clear canvas is unrecoverable, and
 * it is one tap away"). Before this fix, clearAll() emptied the redo
 * stack alongside the stroke list, so a mis-tap on the toolbar's Clear
 * button destroyed a page with no way back.
 *
 * Runs under Robolectric because Stroke.path is androidx.compose.ui.graphics.Path,
 * which delegates to android.graphics.Path on Android — a real framework type a
 * plain JVM test can't construct. This is itself worth noting: docs/19-quality-bar.md
 * §4 called stroke/canvas-state logic "pure logic, no Android deps" as the
 * project's best test target; it turns out to have exactly one Android dependency,
 * inherited from Stroke's use of Path rather than a plain point list
 * (docs/16-canvas-spec.md §8 point 1).
 */
@RunWith(RobolectricTestRunner::class)
class CanvasStateTest {

    private fun CanvasState.drawOneStroke(x: Float = 0f, y: Float = 0f) {
        startStroke(Offset(x, y))
        continueStroke(Offset(x + 10f, y + 10f))
        finishStroke()
    }

    @Test
    fun `clearAll then undoClear restores the exact strokes that were cleared`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.drawOneStroke(50f, 50f)
        state.drawOneStroke(100f, 100f)
        val idsBeforeClear = state.strokes.map { it.id }.toSet()

        state.clearAll()
        assertTrue("strokes should be empty right after clearAll", state.strokes.isEmpty())
        assertTrue("a fresh clear must be undoable", state.canUndoClear())

        state.undoClear()
        assertEquals(3, state.strokes.size)
        assertEquals(idsBeforeClear, state.strokes.map { it.id }.toSet())
        assertFalse("undoClear is single-use", state.canUndoClear())
    }

    @Test
    fun `clearAll on an already-empty page is a no-op and does not clobber an earlier snapshot`() {
        val state = CanvasState()
        state.drawOneStroke()
        state.clearAll()
        assertTrue(state.canUndoClear())

        // Tapping Clear again while the page is already empty must not discard the
        // still-restorable snapshot from the first clear.
        state.clearAll()
        assertTrue(state.canUndoClear())
        state.undoClear()
        assertEquals(1, state.strokes.size)
    }

    @Test
    fun `drawing a new stroke after clearAll makes the clear no longer undoable`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.clearAll()
        assertTrue(state.canUndoClear())

        state.drawOneStroke(200f, 200f)
        assertFalse(
            "restoring the pre-clear snapshot after new ink exists would silently discard it",
            state.canUndoClear()
        )
    }

    @Test
    fun `erasing after clearAll makes the clear no longer undoable`() {
        val state = CanvasState()
        state.drawOneStroke(0f, 0f)
        state.clearAll()
        state.undoClear()
        assertEquals(1, state.strokes.size)

        state.clearAll()
        assertTrue(state.canUndoClear())

        state.drawOneStroke(500f, 500f)
        state.eraseAtPosition(Offset(505f, 505f))
        assertTrue("the erase should have actually removed the stroke", state.strokes.isEmpty())
        assertFalse(state.canUndoClear())
    }

    @Test
    fun `clearAll on an empty canvas leaves nothing to undo`() {
        val state = CanvasState()
        state.clearAll()
        assertFalse(state.canUndoClear())
    }

    @Test
    fun `canUndoClear is false before any clear has happened`() {
        val state = CanvasState()
        assertFalse(state.canUndoClear())
        state.drawOneStroke()
        assertFalse(state.canUndoClear())
    }
}
