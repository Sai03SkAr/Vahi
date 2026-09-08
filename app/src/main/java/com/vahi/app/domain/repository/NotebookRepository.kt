package com.vahi.app.domain.repository

import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.Notebook
import kotlinx.coroutines.flow.Flow

/**
 * Notebook persistence, as seen by the rest of the app. The only Room-aware
 * implementation is `data/repository/RoomNotebookRepository.kt` — nothing above
 * this interface knows Room exists (docs/18-architecture-target.md §3: `domain`
 * has zero Android imports, `ui` depends on interfaces, never entities).
 *
 * Trimmed from the full target contract in docs/17-data-spec.md §9 — no folders,
 * no duplicate, no purge scheduling. Add those when the UI that needs them exists;
 * this covers exactly what `VAHI-207`'s scope needed: create, list, pin, delete
 * with undo.
 */
interface NotebookRepository {

    /** All non-deleted notebooks, most recently updated first. */
    fun observeAll(): Flow<List<Notebook>>

    /** Only pinned, non-deleted notebooks, most recently updated first. */
    fun observePinned(): Flow<List<Notebook>>

    /** Creates and persists a new notebook, returning it (its generated ID is
     *  needed by the caller to navigate straight into the editor).
     *  [colorArgb] is a raw ARGB int — see the note on [Notebook.colorArgb]. */
    suspend fun create(title: String, colorArgb: Int, paperStyle: PaperStyle): Result<Notebook>

    suspend fun setPinned(id: String, pinned: Boolean): Result<Unit>

    /** Soft delete — see docs/17-data-spec.md R3 (tombstone, not a hard delete).
     *  Powers the library's "Undo" snackbar via [restore]. */
    suspend fun delete(id: String): Result<Unit>

    suspend fun restore(id: String): Result<Unit>
}
