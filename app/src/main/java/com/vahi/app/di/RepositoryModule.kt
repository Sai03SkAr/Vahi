package com.vahi.app.di

import com.vahi.app.data.repository.RoomNotebookRepository
import com.vahi.app.domain.repository.NotebookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `@Binds` rather than `@Provides`: RoomNotebookRepository already has an `@Inject`
 *  constructor, so this only needs to tell Hilt which interface it satisfies. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotebookRepository(impl: RoomNotebookRepository): NotebookRepository
}
