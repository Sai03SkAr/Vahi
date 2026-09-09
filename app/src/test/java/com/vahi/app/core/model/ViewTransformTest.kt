package com.vahi.app.core.model


import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private const val EPSILON = 0.001f

class ViewTransformTest {

    @Test
    fun `rejects non-positive scale`() {
        assertThrows(IllegalArgumentException::class.java) { ViewTransform(scale = 0f) }
        assertThrows(IllegalArgumentException::class.java) { ViewTransform(scale = -1f) }
    }

    @Test
    fun `identity transform at 1x density maps document points straight to screen pixels`() {
        val transform = ViewTransform(scale = 1f, pan = DocumentOffset.Zero)
        val density = 1f
        val screen = transform.documentToScreen(DocumentPoint(100f, 200f), density)
        assertEquals(100f, screen.x, EPSILON)
        assertEquals(200f, screen.y, EPSILON)
    }

    @Test
    fun `scale multiplies the screen distance`() {
        val transform = ViewTransform(scale = 2f)
        val density = 1f
        val screen = transform.documentToScreen(DocumentPoint(50f, 0f), density)
        assertEquals(100f, screen.x, EPSILON)
    }

    @Test
    fun `density multiplies the screen distance independently of scale`() {
        val transform = ViewTransform(scale = 1f)
        val density = 3f
        val screen = transform.documentToScreen(DocumentPoint(10f, 0f), density)
        assertEquals(30f, screen.x, EPSILON)
    }

    @Test
    fun `pan shifts the document point before scale and density are applied`() {
        val transform = ViewTransform(scale = 2f, pan = DocumentOffset(dx = 5f, dy = 0f))
        val density = 1f
        // (0 + 5) * 2 * 1 = 10
        val screen = transform.documentToScreen(DocumentPoint(0f, 0f), density)
        assertEquals(10f, screen.x, EPSILON)
    }

    @Test
    fun `screenToDocument is the exact inverse of documentToScreen`() {
        val transform = ViewTransform(scale = 1.75f, pan = DocumentOffset(dx = -12f, dy = 33f))
        val density = 2.5f
        val original = DocumentPoint(123.4f, 567.8f)

        val roundTripped = transform.screenToDocument(
            transform.documentToScreen(original, density),
            density
        )

        assertEquals(original.x, roundTripped.x, EPSILON)
        assertEquals(original.y, roundTripped.y, EPSILON)
    }

    @Test
    fun `screenToDocument at identity is a no-op at 1x density`() {
        val transform = ViewTransform()
        val density = 1f
        val document = transform.screenToDocument(ScreenPoint(42f, 84f), density)
        assertEquals(42f, document.x, EPSILON)
        assertEquals(84f, document.y, EPSILON)
    }
}
