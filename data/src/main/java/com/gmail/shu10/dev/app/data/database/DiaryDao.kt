package com.gmail.shu10.dev.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 日記データのDAO
 */
@Dao
interface DiaryDao {
    /**
     * データ保存（同じIDの場合は上書きして更新）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dairy: DiaryEntity)

    /**
     * 全データ取得
     */
    @Query("SELECT * FROM diaries")
    fun getAllDiaries(): Flow<List<DiaryEntity>>

    /**
     * 日付指定のデータ取得
     */
    @Query("SELECT * FROM diaries WHERE date = :date LIMIT 1")
    fun getDiaryByDate(date: String): Flow<DiaryEntity>
}