package com.vahi.app.di

import android.content.Context
import androidx.room.Room
import com.vahi.app.data.database.VahiDatabase
import com.vahi.app.data.database.dao.NotebookDao
import com.vahi.app.data.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVahiDatabase(@ApplicationContext context: Context): VahiDatabase =
        Room.databaseBuilder(context, VahiDatabase::class.java, VahiDatabase.NAME)
            // No fallbackToDestructiveMigration() — see docs/17-data-spec.md §6.
            // At schema version 1 there is nothing to migrate yet; the first real
            // schema change must ship a Migration, not this.
            .build()

    @Provides
    fun provideNotebookDao(database: VahiDatabase): NotebookDao = database.notebookDao()

    @Provides
    fun provideProfileDao(database: VahiDatabase): ProfileDao = database.profileDao()
}
