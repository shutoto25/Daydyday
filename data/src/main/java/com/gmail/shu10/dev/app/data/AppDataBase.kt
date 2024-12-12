package com.gmail.shu10.dev.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DiaryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}