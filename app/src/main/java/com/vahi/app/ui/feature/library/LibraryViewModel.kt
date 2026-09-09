package com.vahi.app.ui.feature.library

import androidx.lifecycle.viewModelScope
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.Notebook
import com.vahi.app.domain.repository.NotebookRepository
import com.vahi.app.domain.repository.PreferencesRepository
import com.vahi.app.viewmodel.BaseViewModel
import com.vahi.app.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `BaseViewModel`'s first real subclass (it was written and waiting since before
 * Session 1 — docs/09-project-status.md). Backs the library grid with real,
 * persisted notebooks instead of the two hardcoded sample entries.
 *
 * Deliberately simple relative to docs/18-architecture-target.md §4's full target
 * shape: no exhaustive `phase` (Loading/Content/Empty/Error), no one-shot event
 * channel for navigation. `HomeScreen` still calls straight into `hiltViewModel()`
 * itself rather than through a separate stateless-composable + thin-Route split.
 * Both are real gaps, not oversights — see docs/21-backlog.md `VAHI-206` for the
 * fuller rebuild this sets up but does not finish.
 */
data class LibraryUiState(
    val notebooks: List<Notebook> = emptyList(),
    val isLoading: Boolean = true,
    /** The user's remembered default from Settings (`VAHI-307`), used to pre-select
     *  a style in the create-notebook modal (`VAHI-211`). Without this the
     *  `defaultPaperStyle` preference would persist but never be read by anything —
     *  a setting that silently does nothing. */
    val defaultPaperStyle: PaperStyle = PaperStyle.BLANK
) : UiState

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: NotebookRepository,
    private val preferences: PreferencesRepository
) : BaseViewModel<LibraryUiState>(LibraryUiState()) {

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), preferences.observe()) { notebooks, prefs ->
                LibraryUiState(
                    notebooks = notebooks,
                    isLoading = false,
                    defaultPaperStyle = prefs.defaultPaperStyle
                )
            }.collect { setState(it) }
        }
    }

    /** Suspends so the caller (HomeScreen) can navigate straight to the new
     *  notebook's editor on success — see the KDoc on `create` in
     *  `NotebookRepository` for why this returns the created [Notebook]. */
    suspend fun createNotebook(title: String, colorArgb: Int, paperStyle: PaperStyle): Result<Notebook> =
        repository.create(title, colorArgb, paperStyle)

    fun deleteNotebook(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun restoreNotebook(id: String) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun togglePin(id: String, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(id, pinned) }
    }
}
