package com.vahi.app.domain.repository

import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * App-wide settings, as seen by the rest of the app. The only DataStore-aware
 * implementation is `data/preferences/DataStorePreferencesRepository.kt` — same
 * boundary rule as `NotebookRepository`: nothing above this interface knows
 * DataStore exists.
 */
interface PreferencesRepository {

    fun observe(): Flow<UserPreferences>

    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setPaperTheme(theme: PaperTheme)
    suspend fun setHandedness(handedness: Handedness)
    suspend fun setFingerDrawEnabled(enabled: Boolean)
    suspend fun setDefaultPaperStyle(style: PaperStyle)
    suspend fun setLanguageTag(tag: String)
}
