package com.domain.app.di

import com.domain.app.core.data.DataRepository
import com.domain.app.core.storage.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideDataRepository(database: AppDatabase): DataRepository {
        return DataRepository(database)
    }
}
