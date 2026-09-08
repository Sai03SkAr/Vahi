package com.vahi.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.domain.model.UserPreferences
import com.vahi.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * The only class in the app that knows settings are backed by DataStore. Takes the
 * `DataStore<Preferences>` itself rather than a `Context` — `PreferencesModule`
 * provides the real one via the `preferencesDataStore` file delegate (a process-wide
 * singleton keyed by file path, which is awkward to re-create per test); tests just
 * hand this class a throwaway `DataStore<Preferences>` pointed at a temp file, with
 * no Context or Robolectric needed at all.
 *
 * docs/17-data-spec.md §2.4 says "Proto DataStore"; this uses the plain
 * `datastore-preferences` artifact instead (matches docs/18-architecture-target.md
 * §5's artifact table, which lists `datastore-preferences`, not the proto flavour —
 * the two docs disagree). Preferences DataStore is still typed, atomic and
 * `Flow`-observable, which is the requirement the data-spec actually states; Proto
 * DataStore's real advantage — enforced schema evolution — isn't worth a protobuf
 * compiler plugin for six scalar settings. Enums are still stored as their `name`,
 * never an ordinal, for the same reason Room does — see [toAppTheme] etc.
 */
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val PAPER_THEME = stringPreferencesKey("paper_theme")
        val HANDEDNESS = stringPreferencesKey("handedness")
        val FINGER_DRAW_ENABLED = booleanPreferencesKey("finger_draw_enabled")
        val DEFAULT_PAPER_STYLE = stringPreferencesKey("default_paper_style")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }

    override fun observe(): Flow<UserPreferences> =
        dataStore.data
            .catch { error ->
                // Matches Google's own DataStore guidance: an IOException reading the
                // file (e.g. first launch, no file yet) should fall through to
                // defaults, not crash the settings screen. Anything else rethrows.
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { prefs ->
                val defaults = UserPreferences()
                UserPreferences(
                    appTheme = prefs[Keys.APP_THEME].toAppTheme(defaults.appTheme),
                    paperTheme = prefs[Keys.PAPER_THEME].toPaperTheme(defaults.paperTheme),
                    handedness = prefs[Keys.HANDEDNESS].toHandedness(defaults.handedness),
                    fingerDrawEnabled = prefs[Keys.FINGER_DRAW_ENABLED] ?: defaults.fingerDrawEnabled,
                    defaultPaperStyle = prefs[Keys.DEFAULT_PAPER_STYLE].toPaperStyle(defaults.defaultPaperStyle),
                    languageTag = prefs[Keys.LANGUAGE_TAG] ?: defaults.languageTag
                )
            }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    override suspend fun setPaperTheme(theme: PaperTheme) {
        dataStore.edit { it[Keys.PAPER_THEME] = theme.name }
    }

    override suspend fun setHandedness(handedness: Handedness) {
        dataStore.edit { it[Keys.HANDEDNESS] = handedness.name }
    }

    override suspend fun setFingerDrawEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.FINGER_DRAW_ENABLED] = enabled }
    }

    override suspend fun setDefaultPaperStyle(style: PaperStyle) {
        dataStore.edit { it[Keys.DEFAULT_PAPER_STYLE] = style.name }
    }

    override suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }
}

private fun String?.toAppTheme(default: AppTheme): AppTheme =
    this?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: default

private fun String?.toPaperTheme(default: PaperTheme): PaperTheme =
    this?.let { runCatching { PaperTheme.valueOf(it) }.getOrNull() } ?: default

private fun String?.toHandedness(default: Handedness): Handedness =
    this?.let { runCatching { Handedness.valueOf(it) }.getOrNull() } ?: default

private fun String?.toPaperStyle(default: PaperStyle): PaperStyle =
    this?.let { runCatching { PaperStyle.valueOf(it) }.getOrNull() } ?: default
