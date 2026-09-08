package com.vahi.app.core.model

import kotlinx.serialization.Serializable

/**
 * A page's paper style. Moved here from `ui/screens/CanvasScaffold.kt` (where it was
 * named `CanvasType`) when the persistence layer landed (Session 6) — a Room entity
 * and a domain `Notebook` model both need this type, and neither should depend on
 * `ui.screens`. See docs/18-architecture-target.md §2 for the layering this follows.
 *
 * `@Serializable` so it can still be carried directly as a navigation-compose
 * type-safe route argument (`AppRoot.kt`), which is why it started life in `ui/`.
 *
 * The former `NotebookType` enum in the now-deleted `NotebookCanvasScreen.kt` was an
 * identical 1:1 duplicate of this one — see docs/09-project-status.md "Code quality
 * observations".
 */
@Serializable
enum class PaperStyle {
    BLANK,
    RULED,
    GRID
}
