package com.vahi.app.core.model

/** Vertical gap between consecutive pages in the continuous scroll strip, in points. */
const val PAGE_GAP_PT = 24f

/**
 * Lays out an ordered list of pages as a continuous vertical strip in document
 * space — the model behind "vertical continuous scroll"
 * (docs/15-ux-blueprint.md §5.1, docs/16-canvas-spec.md §2.3). Narrower pages are
 * centred against the widest page in the notebook, since imported PDF pages can
 * have different sizes than authored ones (decision D1).
 *
 * All offsets are precomputed in [init] — with 100+ pages in a real notebook,
 * recomputing a prefix sum on every call would make this quietly quadratic.
 */
class PageLayout(private val pageSizes: List<PageSize>) {

    val pageCount: Int get() = pageSizes.size

    private val maxWidthPt: Float = pageSizes.maxOfOrNull { it.widthPt } ?: 0f

    /** Top-left of each page, in document space, indexed the same as [pageSizes]. */
    private val offsets: List<DocumentPoint> = run {
        var y = 0f
        pageSizes.map { size ->
            val x = (maxWidthPt - size.widthPt) / 2f
            val topLeft = DocumentPoint(x, y)
            y += size.heightPt + PAGE_GAP_PT
            topLeft
        }
    }

    /** Top-left of page [index], in document space. */
    fun offsetOf(index: Int): DocumentPoint {
        require(index in pageSizes.indices) { "page index $index out of range for $pageCount pages" }
        return offsets[index]
    }

    /** Total height of the strip: every page plus the gaps between them, but no
     *  trailing gap after the last page. Zero for an empty notebook. */
    fun totalHeightPt(): Float {
        if (pageSizes.isEmpty()) return 0f
        return offsets.last().y + pageSizes.last().heightPt
    }

    /**
     * Which page (if any) contains the given document-space point, and the point
     * re-expressed in that page's own local space. `null` if the point falls in a
     * gap between pages, or outside the strip entirely.
     */
    fun pageAt(point: DocumentPoint): Pair<Int, PagePoint>? {
        for (index in pageSizes.indices) {
            val topLeft = offsets[index]
            val size = pageSizes[index]
            val localX = point.x - topLeft.x
            val localY = point.y - topLeft.y
            if (localX in 0f..size.widthPt && localY in 0f..size.heightPt) {
                return index to PagePoint(localX, localY)
            }
        }
        return null
    }

    /**
     * The document-space point corresponding to a point local to page [index].
     * Does not clamp to the page's bounds — a stroke legitimately spans right up
     * to (and, mid-gesture, briefly past) a page's edge.
     */
    fun toDocumentPoint(index: Int, point: PagePoint): DocumentPoint {
        val topLeft = offsetOf(index)
        return DocumentPoint(topLeft.x + point.x, topLeft.y + point.y)
    }
}
