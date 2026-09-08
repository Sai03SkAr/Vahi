package com.vahi.app.core.model

/**
 * The three coordinate spaces a point can be in, and why they must never be mixed.
 * See docs/16-canvas-spec.md §2 and docs/17-data-spec.md R1 — the hard rule that
 * nothing is written to disk until this model exists, and everything stored must be
 * in [PagePoint], never [ScreenPoint], not even temporarily:
 *
 * ```
 *   screen px  <--ViewTransform-->  document pt  <--PageLayout-->  page pt
 * ```
 *
 * These are distinct types rather than a bare `androidx.compose.ui.geometry.Offset`
 * reused for all three, so the space a point belongs to is part of its type, not a
 * naming convention a future edit can quietly violate — the same class of bug fixed
 * in `DrawingToolbar`'s width-preview mixup (docs/09-project-status.md bug #18), but
 * caught by the compiler instead of by testing.
 */

/** Points per inch — 1 pt = 1/72 in, the document's native unit (matches PDF's, decision D1). */
const val POINTS_PER_INCH = 72f

/**
 * A point in an Android View/Canvas's own pixel space. Raw pointer input arrives here;
 * render calls consume it. **Never stored** — see docs/17-data-spec.md R1.
 */
data class ScreenPoint(val x: Float, val y: Float)

/**
 * A point in the continuous, scrollable document strip, in points. Origin: the
 * top-left of page 0, before any page gap.
 */
data class DocumentPoint(val x: Float, val y: Float) {
    operator fun plus(delta: DocumentOffset): DocumentPoint = DocumentPoint(x + delta.dx, y + delta.dy)
    operator fun minus(other: DocumentPoint): DocumentOffset = DocumentOffset(x - other.x, y - other.y)
}

/** A translation within document space, in points — e.g. the pan component of a [ViewTransform]. */
data class DocumentOffset(val dx: Float, val dy: Float) {
    companion object {
        val Zero = DocumentOffset(0f, 0f)
    }
}

/**
 * A point local to exactly one page, in points. Origin: that page's own top-left.
 * This is the **only** space stroke geometry is ever stored in.
 */
data class PagePoint(val x: Float, val y: Float)

/**
 * A page's dimensions, in points. Per decision D1, the default is A4 portrait, but
 * every page stores its own size — imported PDF pages will have their own.
 */
data class PageSize(val widthPt: Float, val heightPt: Float) {
    init {
        require(widthPt > 0f && heightPt > 0f) {
            "page dimensions must be positive, was ${widthPt}x$heightPt"
        }
    }

    companion object {
        val A4_PORTRAIT = PageSize(widthPt = 595f, heightPt = 842f)
    }
}
