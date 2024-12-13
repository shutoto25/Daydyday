package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.flow.Flow

interface IDiaryRepository {
    suspend fun getDiary(date: String) : Flow<Diary?>
    suspend fun saveDiary(diary: Diary)
}