package com.vahi.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vahi.app.core.model.PaperStyle

/**
 * Room row for a notebook. Trimmed from the full target schema in
 * docs/17-data-spec.md §2.1 — no folder, source type, or thumbnail fields yet,
 * since none of those UI surfaces exist. Add them when they do, not before.
 *
 * `paperStyle` is stored via [com.vahi.app.data.database.Converters] as its enum
 * *name*, never an ordinal (docs/17-data-spec.md R — an ordinal breaks silently
 * the day someone inserts a value in the middle of the enum).
 */
@Entity(
    tableName = "notebooks",
    indices = [Index("updatedAt"), Index("isPinned"), Index("deletedAt")]
)
data class NotebookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val colorArgb: Int,
    val paperStyle: PaperStyle,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    /** Soft-delete marker (docs/17-data-spec.md R3) — null means not deleted.
     *  Powers the library's "Undo" snackbar via a restore, rather than a
     *  confirmation dialog before the delete. */
    val deletedAt: Long? = null
)
