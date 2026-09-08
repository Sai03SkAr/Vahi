package com.vahi.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The local anonymous profile every notebook is stamped with from v1 — created once,
 * on first launch. docs/17-data-spec.md Part 7: getting this row right now is what
 * lets a future sign-in *adopt* existing local data (set [remoteUserId] on this same
 * row) instead of either migrating every notebook or, worse, orphaning them. There is
 * exactly one row in this table until accounts (`VAHI-702`, v2.0) exist.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val remoteUserId: String? = null,
    val displayName: String? = null,
    val createdAt: Long
)
