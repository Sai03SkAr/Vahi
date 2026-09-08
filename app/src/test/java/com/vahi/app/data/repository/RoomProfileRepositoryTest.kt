package com.vahi.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vahi.app.data.database.VahiDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomProfileRepositoryTest {

    private lateinit var database: VahiDatabase
    private lateinit var repository: RoomProfileRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VahiDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomProfileRepository(database.profileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `first call creates the local profile`() = runTest {
        val id = repository.getOrCreateLocalProfileId()

        assertEquals(id, database.profileDao().getLocalProfile()?.id)
    }

    @Test
    fun `second call returns the same id, not a second profile`() = runTest {
        val first = repository.getOrCreateLocalProfileId()
        val second = repository.getOrCreateLocalProfileId()

        assertEquals(first, second)
    }

    @Test
    fun `concurrent first calls still produce exactly one profile — the real race this guards against`() = runTest {
        val ids = (1..20).map { async { repository.getOrCreateLocalProfileId() } }.awaitAll()

        assertEquals(1, ids.toSet().size)
    }
}
