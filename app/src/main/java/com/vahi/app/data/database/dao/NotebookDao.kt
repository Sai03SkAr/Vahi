package com.vahi.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vahi.app.data.database.entity.NotebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isPinned = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<NotebookEntity>>

    @Insert
    suspend fun insert(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    /** Tombstones a notebook rather than deleting the row — see docs/17-data-spec.md R3. */
    @Query("UPDATE notebooks SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("UPDATE notebooks SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)
}
