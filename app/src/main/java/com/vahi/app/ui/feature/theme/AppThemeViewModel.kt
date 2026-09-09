package com.vahi.app.ui.feature.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Supplies the persisted [AppTheme] to `MainActivity`, above `VahiTheme` — the theme
 * has to be decided outside `AppRoot`, since `VahiTheme` wraps it.
 *
 * Not a [com.vahi.app.viewmodel.BaseViewModel] subclass: that base models a screen's
 * `UiState`, and this is a single value feeding the theme wrapper, not a screen.
 *
 * The initial value is [AppTheme.SYSTEM] rather than a null "still loading" state.
 * Blocking the first frame on a disk read would cost time-to-ink for every launch
 * (docs/12-ux-plan.md Part 1) to spare a one-frame theme correction that only a user
 * who has explicitly overridden the system theme would ever see.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    preferences: PreferencesRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = preferences.observe()
        .map { it.appTheme }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM
        )
}
