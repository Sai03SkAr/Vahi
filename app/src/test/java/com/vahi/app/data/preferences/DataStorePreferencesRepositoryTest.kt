package com.vahi.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Runs against a real `DataStore<Preferences>` backed by a temp file, not a mock —
 * same reasoning as `RoomNotebookRepositoryTest`. No Robolectric or Context needed:
 * the repository was deliberately written to take a `DataStore<Preferences>`
 * directly rather than a Context, so this is a plain JVM test.
 */
class DataStorePreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: DataStorePreferencesRepository

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test.preferences_pb") }
        )
        repository = DataStorePreferencesRepository(dataStore)
    }

    @Test
    fun `observe returns defaults before anything is ever written`() = runTest {
        assertEquals(UserPreferences(), repository.observe().first())
    }

    @Test
    fun `setAppTheme persists and observe reflects it`() = runTest {
        repository.setAppTheme(AppTheme.DARK)

        assertEquals(AppTheme.DARK, repository.observe().first().appTheme)
    }

    @Test
    fun `setPaperTheme persists independently of app theme — V1`() = runTest {
        repository.setAppTheme(AppTheme.DARK)
        repository.setPaperTheme(PaperTheme.CREAM)

        val prefs = repository.observe().first()
        assertEquals(AppTheme.DARK, prefs.appTheme)
        assertEquals(PaperTheme.CREAM, prefs.paperTheme)
    }

    @Test
    fun `setHandedness persists`() = runTest {
        repository.setHandedness(Handedness.LEFT)

        assertEquals(Handedness.LEFT, repository.observe().first().handedness)
    }

    @Test
    fun `setFingerDrawEnabled persists false, overriding the finger-first default`() = runTest {
        repository.setFingerDrawEnabled(false)

        assertEquals(false, repository.observe().first().fingerDrawEnabled)
    }

    @Test
    fun `setDefaultPaperStyle persists`() = runTest {
        repository.setDefaultPaperStyle(PaperStyle.GRID)

        assertEquals(PaperStyle.GRID, repository.observe().first().defaultPaperStyle)
    }

    @Test
    fun `setLanguageTag persists`() = runTest {
        repository.setLanguageTag("hi")

        assertEquals("hi", repository.observe().first().languageTag)
    }

    @Test
    fun `each setter is independent — setting one field doesn't disturb the others`() = runTest {
        repository.setAppTheme(AppTheme.LIGHT)
        repository.setHandedness(Handedness.LEFT)
        repository.setFingerDrawEnabled(false)

        repository.setDefaultPaperStyle(PaperStyle.RULED)

        val prefs = repository.observe().first()
        assertEquals(AppTheme.LIGHT, prefs.appTheme)
        assertEquals(Handedness.LEFT, prefs.handedness)
        assertEquals(false, prefs.fingerDrawEnabled)
        assertEquals(PaperStyle.RULED, prefs.defaultPaperStyle)
    }
}
