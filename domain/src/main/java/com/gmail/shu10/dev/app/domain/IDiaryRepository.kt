package com.gmail.shu10.dev.app.domain

interface IDiaryRepository {

    suspend fun saveDiary(diary: Diary)
}