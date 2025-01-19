package com.gmail.shu10.dev.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DiaryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}