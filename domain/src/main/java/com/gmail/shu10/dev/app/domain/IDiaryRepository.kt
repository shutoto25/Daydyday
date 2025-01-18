package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.flow.Flow

interface IDiaryRepository {
    suspend fun saveDiary(diary: Diary)
    fun getAllDiaries(): Flow<List<Diary>>
    fun getDiaryByDate(date: String) : Flow<Diary>
}