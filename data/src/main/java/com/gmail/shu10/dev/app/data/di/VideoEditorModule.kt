package com.gmail.shu10.dev.app.data.di

import com.gmail.shu10.dev.app.data.VideoEditorRepositoryImpl
import com.gmail.shu10.dev.app.domain.IVideoEditorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 動画編集DI設定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VideoEditorModule {

    @Binds
    @Singleton
    abstract fun bindVideoEditorRepository(
        impl: VideoEditorRepositoryImpl
    ): IVideoEditorRepository
}