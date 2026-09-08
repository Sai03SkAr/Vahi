package com.vahi.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordinateSpacesTest {

    @Test
    fun `PageSize rejects non-positive width or height`() {
        assertThrows(IllegalArgumentException::class.java) { PageSize(0f, 842f) }
        assertThrows(IllegalArgumentException::class.java) { PageSize(595f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { PageSize(-10f, 842f) }
    }

    @Test
    fun `A4_PORTRAIT is 595 by 842 points`() {
        assertEquals(595f, PageSize.A4_PORTRAIT.widthPt, 0f)
        assertEquals(842f, PageSize.A4_PORTRAIT.heightPt, 0f)
    }

    @Test
    fun `DocumentPoint plus DocumentOffset translates both axes`() {
        val start = DocumentPoint(10f, 20f)
        val moved = start + DocumentOffset(dx = 5f, dy = -3f)
        assertEquals(15f, moved.x, 0f)
        assertEquals(17f, moved.y, 0f)
    }

    @Test
    fun `DocumentPoint minus DocumentPoint yields the offset between them`() {
        val a = DocumentPoint(10f, 20f)
        val b = DocumentPoint(4f, 25f)
        val delta = a - b
        assertEquals(6f, delta.dx, 0f)
        assertEquals(-5f, delta.dy, 0f)
    }

    @Test
    fun `DocumentOffset Zero is the additive identity`() {
        val point = DocumentPoint(3f, 4f)
        assertEquals(point, point + DocumentOffset.Zero)
    }
}
