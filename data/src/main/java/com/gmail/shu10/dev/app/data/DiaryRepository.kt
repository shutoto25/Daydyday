package com.gmail.shu10.dev.app.data

import com.gmail.shu10.dev.app.data.database.DiaryDao
import com.gmail.shu10.dev.app.data.database.DiaryEntity
import com.gmail.shu10.dev.app.domain.IDiaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DiaryRepository @Inject constructor(private val diaryDao: DiaryDao) : IDiaryRepository {

    fun getAllDiaries(): Flow<List<DiaryEntity>> = diaryDao.getAllDiaries()

    override fun addDiary() {
        TODO("Not yet implemented")
    }
}