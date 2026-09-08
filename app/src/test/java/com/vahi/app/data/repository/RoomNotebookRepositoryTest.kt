package com.vahi.app.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.data.database.VahiDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs against a real, in-memory Room database — not a mock — so it verifies the
 * actual SQL (ordering, the `deletedAt IS NULL` filter, the tombstone semantics),
 * not just that the repository calls the DAO. Robolectric supplies the Context
 * Room's in-memory builder needs.
 */
@RunWith(RobolectricTestRunner::class)
class RoomNotebookRepositoryTest {

    private lateinit var database: VahiDatabase
    private lateinit var repository: RoomNotebookRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VahiDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomNotebookRepository(database.notebookDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create persists a notebook that observeAll then returns`() = runTest {
        val result = repository.create("Maths notes", Color(0xFFBABEFF), PaperStyle.RULED)
        assertTrue(result.isSuccess)

        val notebooks = repository.observeAll().first()
        assertEquals(1, notebooks.size)
        assertEquals("Maths notes", notebooks.first().title)
        assertEquals(PaperStyle.RULED, notebooks.first().paperStyle)
    }

    @Test
    fun `observeAll excludes soft-deleted notebooks`() = runTest {
        val notebook = repository.create("Physics", Color.Red, PaperStyle.BLANK).getOrThrow()

        repository.delete(notebook.id)

        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun `restore brings a soft-deleted notebook back — the Undo snackbar's mechanism`() = runTest {
        val notebook = repository.create("Chemistry", Color.Blue, PaperStyle.GRID).getOrThrow()
        repository.delete(notebook.id)

        repository.restore(notebook.id)

        val notebooks = repository.observeAll().first()
        assertEquals(1, notebooks.size)
        assertEquals(notebook.id, notebooks.first().id)
    }

    @Test
    fun `setPinned updates isPinned and observePinned reflects it`() = runTest {
        val notebook = repository.create("Biology", Color.Green, PaperStyle.BLANK).getOrThrow()
        assertTrue(repository.observePinned().first().isEmpty())

        repository.setPinned(notebook.id, true)

        val pinned = repository.observePinned().first()
        assertEquals(1, pinned.size)
        assertTrue(pinned.first().isPinned)
    }

    @Test
    fun `unpinning removes it from observePinned but leaves it in observeAll`() = runTest {
        val notebook = repository.create("History", Color.Yellow, PaperStyle.BLANK).getOrThrow()
        repository.setPinned(notebook.id, true)

        repository.setPinned(notebook.id, false)

        assertTrue(repository.observePinned().first().isEmpty())
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun `observeAll orders by most recently updated first`() = runTest {
        val first = repository.create("First", Color.Red, PaperStyle.BLANK).getOrThrow()
        Thread.sleep(5) // ensure a distinct updatedAt millisecond
        val second = repository.create("Second", Color.Blue, PaperStyle.BLANK).getOrThrow()

        val notebooks = repository.observeAll().first()
        assertEquals(listOf(second.id, first.id), notebooks.map { it.id })
    }

    @Test
    fun `pinning a notebook bumps it to the top of observeAll too`() = runTest {
        val first = repository.create("Older", Color.Red, PaperStyle.BLANK).getOrThrow()
        Thread.sleep(5)
        repository.create("Newer", Color.Blue, PaperStyle.BLANK).getOrThrow()

        repository.setPinned(first.id, true) // touches updatedAt

        assertEquals(first.id, repository.observeAll().first().first().id)
    }

    @Test
    fun `colour round-trips exactly through ARGB storage`() = runTest {
        val original = Color(0xFF7C4DFF)
        repository.create("Colour check", original, PaperStyle.BLANK)

        val stored = repository.observeAll().first().first().color

        assertEquals(original.toArgb(), stored.toArgb())
    }

    @Test
    fun `deleting an already-deleted notebook is harmless`() = runTest {
        val notebook = repository.create("Redundant delete", Color.Gray, PaperStyle.BLANK).getOrThrow()
        repository.delete(notebook.id)

        val result = repository.delete(notebook.id)

        assertTrue(result.isSuccess)
        assertTrue(repository.observeAll().first().isEmpty())
    }
}
