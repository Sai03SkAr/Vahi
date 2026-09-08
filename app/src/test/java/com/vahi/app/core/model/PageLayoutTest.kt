package com.vahi.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

private const val EPSILON = 0.001f
private val A4 = PageSize.A4_PORTRAIT // 595 x 842

class PageLayoutTest {

    @Test
    fun `empty notebook has zero pages, zero height, and nothing at any point`() {
        val layout = PageLayout(emptyList())
        assertEquals(0, layout.pageCount)
        assertEquals(0f, layout.totalHeightPt(), 0f)
        assertNull(layout.pageAt(DocumentPoint(0f, 0f)))
    }

    @Test
    fun `offsetOf rejects an out-of-range index`() {
        val layout = PageLayout(listOf(A4))
        assertThrows(IllegalArgumentException::class.java) { layout.offsetOf(1) }
        assertThrows(IllegalArgumentException::class.java) { layout.offsetOf(-1) }
    }

    @Test
    fun `a single page sits at the origin and the strip height equals the page height`() {
        val layout = PageLayout(listOf(A4))
        val offset = layout.offsetOf(0)
        assertEquals(0f, offset.x, 0f)
        assertEquals(0f, offset.y, 0f)
        assertEquals(842f, layout.totalHeightPt(), 0f)
    }

    @Test
    fun `same-width pages stack vertically with PAGE_GAP_PT between them`() {
        val layout = PageLayout(listOf(A4, A4))
        val secondPageOffset = layout.offsetOf(1)
        assertEquals(0f, secondPageOffset.x, 0f)
        assertEquals(842f + PAGE_GAP_PT, secondPageOffset.y, 0f)
        assertEquals(842f * 2 + PAGE_GAP_PT, layout.totalHeightPt(), 0f)
    }

    @Test
    fun `three pages accumulate height across all preceding pages and gaps`() {
        val layout = PageLayout(listOf(A4, A4, A4))
        val thirdPageOffset = layout.offsetOf(2)
        assertEquals(2 * (842f + PAGE_GAP_PT), thirdPageOffset.y, 0f)
    }

    @Test
    fun `a narrower page is centred against the widest page in the notebook`() {
        val narrower = PageSize(widthPt = 400f, heightPt = 600f)
        val layout = PageLayout(listOf(A4, narrower))
        val narrowerOffset = layout.offsetOf(1)
        assertEquals((595f - 400f) / 2f, narrowerOffset.x, EPSILON)
        assertEquals(842f + PAGE_GAP_PT, narrowerOffset.y, 0f)
        assertEquals(842f + PAGE_GAP_PT + 600f, layout.totalHeightPt(), 0f)
    }

    @Test
    fun `pageAt finds a point on the first page and converts it to page-local space`() {
        val layout = PageLayout(listOf(A4, A4))
        val result = layout.pageAt(DocumentPoint(100f, 100f))
        assertEquals(0 to PagePoint(100f, 100f), result)
    }

    @Test
    fun `pageAt finds a point on the second page, accounting for the first page's height`() {
        val layout = PageLayout(listOf(A4, A4))
        // Second page starts at y = 866; a point at document y = 900 is 34pt into it.
        val result = layout.pageAt(DocumentPoint(100f, 900f))
        assertEquals(1 to PagePoint(100f, 34f), result)
    }

    @Test
    fun `pageAt returns null for a point in the gap between pages`() {
        val layout = PageLayout(listOf(A4, A4))
        // Page 0 ends at y=842; page 1 starts at y=866. y=850 is in the gap.
        assertNull(layout.pageAt(DocumentPoint(100f, 850f)))
    }

    @Test
    fun `pageAt returns null above the first page and below the last page`() {
        val layout = PageLayout(listOf(A4, A4))
        assertNull(layout.pageAt(DocumentPoint(100f, -10f)))
        assertNull(layout.pageAt(DocumentPoint(100f, 2000f)))
    }

    @Test
    fun `pageAt returns null for a point beyond a page's width even at a valid y`() {
        val layout = PageLayout(listOf(A4))
        assertNull(layout.pageAt(DocumentPoint(600f, 100f))) // page is only 595pt wide
    }

    @Test
    fun `toDocumentPoint is the inverse of pageAt's local-space conversion`() {
        val layout = PageLayout(listOf(A4, A4))
        val original = DocumentPoint(100f, 900f)
        val (index, local) = layout.pageAt(original)!!
        val roundTripped = layout.toDocumentPoint(index, local)
        assertEquals(original.x, roundTripped.x, EPSILON)
        assertEquals(original.y, roundTripped.y, EPSILON)
    }
}
