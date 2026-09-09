package com.vahi.app.ui.feature.library

import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.Notebook
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.domain.model.UserPreferences
import com.vahi.app.domain.repository.NotebookRepository
import com.vahi.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The first ViewModel-level test in the project. Uses hand-written fakes rather than
 * a mocking library — `mockk`/`turbine` are named in
 * docs/18-architecture-target.md §5 but are not actually declared dependencies yet,
 * and two small fakes are cheaper than adding two artifacts for this.
 *
 * The behaviour that most needs covering here is the `defaultPaperStyle` wiring: the
 * preference is written by the Settings screen, and if nothing reads it back into
 * library state, the setting silently does nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeNotebookRepository : NotebookRepository {
        val notebooks = MutableStateFlow<List<Notebook>>(emptyList())
        override fun observeAll(): Flow<List<Notebook>> = notebooks
        override fun observePinned(): Flow<List<Notebook>> = notebooks.map { list -> list.filter { it.isPinned } }
        override suspend fun create(title: String, colorArgb: Int, paperStyle: PaperStyle): Result<Notebook> {
            val notebook = Notebook(
                id = Notebook.newId(),
                title = title,
                colorArgb = colorArgb,
                paperStyle = paperStyle,
                createdAt = 0L,
                updatedAt = 0L
            )
            notebooks.value = notebooks.value + notebook
            return Result.success(notebook)
        }
        override suspend fun setPinned(id: String, pinned: Boolean): Result<Unit> {
            notebooks.value = notebooks.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> {
            notebooks.value = notebooks.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
        override suspend fun restore(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakePreferencesRepository : PreferencesRepository {
        val preferences = MutableStateFlow(UserPreferences())
        override fun observe(): Flow<UserPreferences> = preferences
        override suspend fun setAppTheme(theme: AppTheme) {
            preferences.value = preferences.value.copy(appTheme = theme)
        }
        override suspend fun setPaperTheme(theme: PaperTheme) {
            preferences.value = preferences.value.copy(paperTheme = theme)
        }
        override suspend fun setHandedness(handedness: Handedness) {
            preferences.value = preferences.value.copy(handedness = handedness)
        }
        override suspend fun setFingerDrawEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(fingerDrawEnabled = enabled)
        }
        override suspend fun setDefaultPaperStyle(style: PaperStyle) {
            preferences.value = preferences.value.copy(defaultPaperStyle = style)
        }
        override suspend fun setLanguageTag(tag: String) {
            preferences.value = preferences.value.copy(languageTag = tag)
        }
    }

    private lateinit var notebooks: FakeNotebookRepository
    private lateinit var preferences: FakePreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        notebooks = FakeNotebookRepository()
        preferences = FakePreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = LibraryViewModel(notebooks, preferences)

    @Test
    fun `starts in the loading state before the first emission is collected`() = runTest(dispatcher) {
        val vm = viewModel()

        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `notebooks from the repository reach ui state, and loading clears`() = runTest(dispatcher) {
        notebooks.create("Maths notes", 0xFFBABEFF.toInt(), PaperStyle.RULED)
        val vm = viewModel()

        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("Maths notes"), vm.uiState.value.notebooks.map { it.title })
    }

    @Test
    fun `the saved default paper style reaches ui state — the Settings control is not inert`() = runTest(dispatcher) {
        preferences.setDefaultPaperStyle(PaperStyle.GRID)
        val vm = viewModel()

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PaperStyle.GRID, vm.uiState.value.defaultPaperStyle)
    }

    @Test
    fun `changing the default paper style later is picked up without recreating the ViewModel`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(PaperStyle.BLANK, vm.uiState.value.defaultPaperStyle)

        preferences.setDefaultPaperStyle(PaperStyle.RULED)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PaperStyle.RULED, vm.uiState.value.defaultPaperStyle)
    }

    @Test
    fun `deleting a notebook removes it from ui state`() = runTest(dispatcher) {
        val created = notebooks.create("Physics", 0xFFFF0000.toInt(), PaperStyle.BLANK).getOrThrow()
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.deleteNotebook(created.id)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.notebooks.isEmpty())
    }

    @Test
    fun `togglePin is reflected in ui state`() = runTest(dispatcher) {
        val created = notebooks.create("Chemistry", 0xFF00FF00.toInt(), PaperStyle.BLANK).getOrThrow()
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.togglePin(created.id, true)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.notebooks.single().isPinned)
    }
}
