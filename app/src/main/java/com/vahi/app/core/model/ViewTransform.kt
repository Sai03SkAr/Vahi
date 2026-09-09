package com.vahi.app.core.model

/**
 * The scale + pan applied between document space and screen space. Rotation is out
 * of scope for v1 (docs/16-canvas-spec.md §2.2) but the shape leaves room for a third
 * field later without changing every call site.
 */
data class ViewTransform(
    val scale: Float = 1f,
    val pan: DocumentOffset = DocumentOffset.Zero
) {
    init {
        require(scale > 0f) { "scale must be positive, was $scale" }
    }
}

/** Absolute zoom ceiling (docs/16-canvas-spec.md §2.2). Per-document minimum (fit-to-width)
 *  is computed at the call site, since it depends on the live viewport size. */
const val MAX_VIEW_SCALE = 8f

/** Never let a page render smaller than this fraction of the viewport width — below it,
 *  users lose their place. Enforced where zoom gestures clamp `scale`, not here. */
const val MIN_VIEW_SCALE_PAGE_FRACTION = 0.3f

/**
 * [pixelsPerDp] is Compose's `Density.density` — passed as a plain `Float` rather than
 * as a `Density`, so this package keeps its "pure Kotlin, zero Android/Compose
 * imports" property (docs/18-architecture-target.md §3). Only the `.density` field was
 * ever read, so the dependency bought nothing; call sites pass
 * `LocalDensity.current.density`. This also matters for
 * docs/23-cross-platform-plan.md Axis B, where `core/` is meant to move into
 * `commonMain` untouched.
 */
fun ViewTransform.documentToScreen(point: DocumentPoint, pixelsPerDp: Float): ScreenPoint {
    val effectiveScale = scale * pixelsPerDp
    return ScreenPoint(
        x = (point.x + pan.dx) * effectiveScale,
        y = (point.y + pan.dy) * effectiveScale
    )
}

fun ViewTransform.screenToDocument(point: ScreenPoint, pixelsPerDp: Float): DocumentPoint {
    val effectiveScale = scale * pixelsPerDp
    return DocumentPoint(
        x = point.x / effectiveScale - pan.dx,
        y = point.y / effectiveScale - pan.dy
    )
}
