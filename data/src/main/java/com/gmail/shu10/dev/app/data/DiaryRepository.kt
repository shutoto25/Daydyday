package com.gmail.shu10.dev.app.data

import android.util.Log
import com.gmail.shu10.dev.app.data.database.DiaryDao
import com.gmail.shu10.dev.app.data.mapper.toDomain
import com.gmail.shu10.dev.app.data.mapper.toEntity
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.IDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 日記データのリポジトリ
 */
class DiaryRepository @Inject constructor(private val diaryDao: DiaryDao) : IDiaryRepository {

    override suspend fun saveDiary(diary: Diary) {
        Log.d("TEST", "saveDiary() called with: diary = $diary")
        val entity = diary.toEntity()
        diaryDao.insert(entity)
    }

    override fun getAllDiaries(): Flow<List<Diary>> {
        return diaryDao.getAllDiaries().map { entities ->
            entities.map { entity ->
                entity.toDomain()
            }
        }
    }

    override fun getDiaryByDate(date: String): Flow<Diary> {
        return diaryDao.getDiaryByDate(date).map { entity ->
            entity.toDomain()
        }
    }
}