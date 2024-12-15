package com.gmail.shu10.dev.app.data

import com.gmail.shu10.dev.app.data.database.DiaryDao
import com.gmail.shu10.dev.app.data.mapper.toDomain
import com.gmail.shu10.dev.app.data.mapper.toEntity
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.IDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DiaryRepository @Inject constructor(private val diaryDao: DiaryDao) : IDiaryRepository {

    override suspend fun saveDiary(diary: Diary) {
        diaryDao.insert(diary.toEntity())
    }

    override fun getDiaryByDate(date: String): Flow<Diary?> {
        return diaryDao.getDiaryByDate(date).map { entity ->
            entity?.toDomain()
        }
    }
}