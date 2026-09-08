package com.vahi.app.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.data.database.dao.NotebookDao
import com.vahi.app.data.database.entity.NotebookEntity
import com.vahi.app.domain.model.Notebook
import com.vahi.app.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The only class in the app that knows notebooks are backed by Room. Everything
 * above [NotebookRepository] — ViewModels, `ui/` — depends on the interface only
 * (docs/18-architecture-target.md §3).
 */
class RoomNotebookRepository @Inject constructor(
    private val dao: NotebookDao
) : NotebookRepository {

    override fun observeAll(): Flow<List<Notebook>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observePinned(): Flow<List<Notebook>> =
        dao.observePinned().map { entities -> entities.map { it.toDomain() } }

    override suspend fun create(title: String, color: Color, paperStyle: PaperStyle): Result<Notebook> =
        runCatching {
            val now = System.currentTimeMillis()
            val entity = NotebookEntity(
                id = Notebook.newId(),
                title = title,
                colorArgb = color.toArgb(),
                paperStyle = paperStyle,
                createdAt = now,
                updatedAt = now
            )
            dao.insert(entity)
            entity.toDomain()
        }

    override suspend fun setPinned(id: String, pinned: Boolean): Result<Unit> =
        runCatching { dao.setPinned(id, pinned, System.currentTimeMillis()) }

    override suspend fun delete(id: String): Result<Unit> =
        runCatching { dao.softDelete(id, System.currentTimeMillis()) }

    override suspend fun restore(id: String): Result<Unit> =
        runCatching { dao.restore(id) }
}

private fun NotebookEntity.toDomain(): Notebook = Notebook(
    id = id,
    title = title,
    color = Color(colorArgb),
    paperStyle = paperStyle,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned
)
