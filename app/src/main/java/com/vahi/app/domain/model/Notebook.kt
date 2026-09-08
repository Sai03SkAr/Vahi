package com.vahi.app.domain.model

import com.vahi.app.core.model.PaperStyle
import java.util.UUID

/**
 * A notebook: the library's core object. This is the domain model — it is never a
 * Room entity, and nothing in `ui/` should reach past a repository or ViewModel to
 * touch persistence directly.
 *
 * Trimmed from the full target schema in docs/17-data-spec.md §2.1: no page or
 * stroke fields yet, because a notebook currently has exactly one implicit
 * "page" — the ink layer — and that still isn't persisted (blocked on the
 * `androidx.ink` decision, D6, and the latency benchmark, `VAHI-101`). Adding a
 * real multi-page model is `VAHI-401`, later.
 *
 * [colorArgb] is a raw ARGB int rather than a Compose `Color`: docs/18-architecture-target.md
 * §3 requires this layer to carry no UI-framework dependency, and the Room column is
 * already an int, so this also removes a pointless Color↔int round-trip. The UI converts
 * at its own boundary (`HomeScreen`), which is where a presentation type belongs.
 */
data class Notebook(
    val id: String,
    val title: String,
    val colorArgb: Int,
    val paperStyle: PaperStyle,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
