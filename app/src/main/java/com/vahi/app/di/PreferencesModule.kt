package com.vahi.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.vahi.app.data.preferences.DataStorePreferencesRepository
import com.vahi.app.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** Kept separate from `RepositoryModule` — matches the module split named in
 *  docs/18-architecture-target.md §4 (`DatabaseModule` / `RepositoryModule` /
 *  `PreferencesModule`), and preferences aren't a `NotebookRepository`-shaped
 *  concern. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.userPreferencesDataStore
    }
}
