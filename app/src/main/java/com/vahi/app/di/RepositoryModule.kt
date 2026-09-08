package com.vahi.app.di

import com.vahi.app.data.repository.RoomNotebookRepository
import com.vahi.app.data.repository.RoomProfileRepository
import com.vahi.app.domain.repository.NotebookRepository
import com.vahi.app.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `@Binds` rather than `@Provides`: the implementations already have an `@Inject`
 *  constructor, so this only needs to tell Hilt which interface each satisfies. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotebookRepository(impl: RoomNotebookRepository): NotebookRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RoomProfileRepository): ProfileRepository
}
