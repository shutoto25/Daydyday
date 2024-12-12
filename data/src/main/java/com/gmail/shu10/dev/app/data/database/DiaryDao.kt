package com.gmail.shu10.dev.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Insert
    suspend fun insert(dairy: DiaryEntity)

    @Query("SELECT * FROM diaries ORDER BY id DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>>
}