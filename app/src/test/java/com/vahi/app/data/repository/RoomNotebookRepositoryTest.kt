package com.vahi.app.data.repository

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
        repository = RoomNotebookRepository(database.notebookDao(), RoomProfileRepository(database.profileDao()))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create persists a notebook that observeAll then returns`() = runTest {
        val result = repository.create("Maths notes", 0xFFBABEFF.toInt(), PaperStyle.RULED)
        assertTrue(result.isSuccess)

        val notebooks = repository.observeAll().first()
        assertEquals(1, notebooks.size)
        assertEquals("Maths notes", notebooks.first().title)
        assertEquals(PaperStyle.RULED, notebooks.first().paperStyle)
    }

    @Test
    fun `observeAll excludes soft-deleted notebooks`() = runTest {
        val notebook = repository.create("Physics", 0xFFFF0000.toInt(), PaperStyle.BLANK).getOrThrow()

        repository.delete(notebook.id)

        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun `restore brings a soft-deleted notebook back — the Undo snackbar's mechanism`() = runTest {
        val notebook = repository.create("Chemistry", 0xFF0000FF.toInt(), PaperStyle.GRID).getOrThrow()
        repository.delete(notebook.id)

        repository.restore(notebook.id)

        val notebooks = repository.observeAll().first()
        assertEquals(1, notebooks.size)
        assertEquals(notebook.id, notebooks.first().id)
    }

    @Test
    fun `setPinned updates isPinned and observePinned reflects it`() = runTest {
        val notebook = repository.create("Biology", 0xFF00FF00.toInt(), PaperStyle.BLANK).getOrThrow()
        assertTrue(repository.observePinned().first().isEmpty())

        repository.setPinned(notebook.id, true)

        val pinned = repository.observePinned().first()
        assertEquals(1, pinned.size)
        assertTrue(pinned.first().isPinned)
    }

    @Test
    fun `unpinning removes it from observePinned but leaves it in observeAll`() = runTest {
        val notebook = repository.create("History", 0xFFFFFF00.toInt(), PaperStyle.BLANK).getOrThrow()
        repository.setPinned(notebook.id, true)

        repository.setPinned(notebook.id, false)

        assertTrue(repository.observePinned().first().isEmpty())
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun `observeAll orders by most recently updated first`() = runTest {
        val first = repository.create("First", 0xFFFF0000.toInt(), PaperStyle.BLANK).getOrThrow()
        Thread.sleep(5) // ensure a distinct updatedAt millisecond
        val second = repository.create("Second", 0xFF0000FF.toInt(), PaperStyle.BLANK).getOrThrow()

        val notebooks = repository.observeAll().first()
        assertEquals(listOf(second.id, first.id), notebooks.map { it.id })
    }

    @Test
    fun `pinning a notebook bumps it to the top of observeAll too`() = runTest {
        val first = repository.create("Older", 0xFFFF0000.toInt(), PaperStyle.BLANK).getOrThrow()
        Thread.sleep(5)
        repository.create("Newer", 0xFF0000FF.toInt(), PaperStyle.BLANK).getOrThrow()

        repository.setPinned(first.id, true) // touches updatedAt

        assertEquals(first.id, repository.observeAll().first().first().id)
    }

    @Test
    fun `colour round-trips exactly through ARGB storage`() = runTest {
        // Deliberately a value with a set alpha channel and a high bit in each
        // component — a naive Int/Long conversion would corrupt this one.
        val original = 0xFF7C4DFF.toInt()
        repository.create("Colour check", original, PaperStyle.BLANK)

        val stored = repository.observeAll().first().first().colorArgb

        assertEquals(original, stored)
    }

    @Test
    fun `deleting an already-deleted notebook is harmless`() = runTest {
        val notebook = repository.create("Redundant delete", 0xFF888888.toInt(), PaperStyle.BLANK).getOrThrow()
        repository.delete(notebook.id)

        val result = repository.delete(notebook.id)

        assertTrue(result.isSuccess)
        assertTrue(repository.observeAll().first().isEmpty())
    }
}
