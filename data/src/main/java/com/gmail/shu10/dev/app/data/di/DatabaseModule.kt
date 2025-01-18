package com.gmail.shu10.dev.app.data.di

import android.content.Context
import androidx.room.Room
import com.gmail.shu10.dev.app.data.database.AppDataBase
import com.gmail.shu10.dev.app.data.database.DiaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * データベース関連のDI設定
 */
@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    /**
     * データベースを提供
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDataBase {
        return Room.databaseBuilder(
            context,
            AppDataBase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * 日記データのDAOを提供
     */
    @Provides
    fun provideDiaryDao(database: AppDataBase): DiaryDao {
        return database.diaryDao()
    }
}