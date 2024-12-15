package com.gmail.shu10.dev.app.data.di

import com.gmail.shu10.dev.app.data.DiaryRepository
import com.gmail.shu10.dev.app.data.database.DiaryDao
import com.gmail.shu10.dev.app.domain.IDiaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideDiaryRepository(diaryDao: DiaryDao): IDiaryRepository {
        return DiaryRepository(diaryDao)
    }
}