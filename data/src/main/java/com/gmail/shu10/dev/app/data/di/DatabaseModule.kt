package com.gmail.shu10.dev.app.data.di

import android.content.Context
import androidx.room.Room
import com.gmail.shu10.dev.app.data.database.AppDataBase
import com.gmail.shu10.dev.app.data.database.DiaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context): AppDataBase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDataBase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideDiaryDao(database: AppDataBase): DiaryDao {
        return database.diaryDao()
    }
}