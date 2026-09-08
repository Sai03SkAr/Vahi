package com.vahi.app.ui.feature.settings

import androidx.lifecycle.viewModelScope
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.data.database.VahiDatabase
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.domain.model.UserPreferences
import com.vahi.app.domain.repository.NotebookRepository
import com.vahi.app.domain.repository.PreferencesRepository
import com.vahi.app.viewmodel.BaseViewModel
import com.vahi.app.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * `VAHI-406` — real settings, replacing `SettingsOverlay`'s four grey placeholder
 * rows. Scoped to docs/21-backlog.md `VAHI-406`'s own "Do" line: handedness, paper
 * theme, finger-draw, storage, about — not the full docs/15-ux-blueprint.md §6
 * table. Account and Vahi Plus are omitted entirely rather than built as no-op
 * placeholders — neither auth nor billing exists yet (both v2.0), and CLAUDE.md's
 * own failure-mode table calls out wiring no-op buttons as work thrown away twice.
 */
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val notebookCount: Int = 0,
    /** Room's on-disk SQLite file size, in bytes — a real number, not a per-notebook
     *  breakdown. A true per-notebook figure needs ink to actually persist
     *  (`VAHI-302`/`VAHI-305`); until then every notebook is just a metadata row and
     *  a per-notebook split would be close to meaningless. */
    val databaseSizeBytes: Long = 0L,
    val isLoading: Boolean = true
) : UiState

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    notebookRepository: NotebookRepository,
    @ApplicationContext context: android.content.Context
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            // File.length() is blocking I/O — the ViewModel's init runs on whichever
            // thread first calls hiltViewModel() (Compose's main thread), so this
            // can't run inline here.
            val databaseSizeBytes = withContext(Dispatchers.IO) {
                context.getDatabasePath(VahiDatabase.NAME).length()
            }
            combine(preferencesRepository.observe(), notebookRepository.observeAll()) { prefs, notebooks ->
                SettingsUiState(
                    preferences = prefs,
                    notebookCount = notebooks.size,
                    databaseSizeBytes = databaseSizeBytes,
                    isLoading = false
                )
            }.collect { setState(it) }
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesRepository.setAppTheme(theme) }
    }

    fun setPaperTheme(theme: PaperTheme) {
        viewModelScope.launch { preferencesRepository.setPaperTheme(theme) }
    }

    fun setDefaultPaperStyle(style: PaperStyle) {
        viewModelScope.launch { preferencesRepository.setDefaultPaperStyle(style) }
    }

    fun setHandedness(handedness: Handedness) {
        viewModelScope.launch { preferencesRepository.setHandedness(handedness) }
    }

    fun setFingerDrawEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFingerDrawEnabled(enabled) }
    }
}
