package com.gmail.shu10.dev.app.daybyday.di

import com.gmail.shu10.dev.app.data.SharedPreferenceRepository
import com.gmail.shu10.dev.app.domain.ISharedPreferenceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides
    fun provideSharedPreferenceRepository(): ISharedPreferenceRepository = SharedPreferenceRepository()
}