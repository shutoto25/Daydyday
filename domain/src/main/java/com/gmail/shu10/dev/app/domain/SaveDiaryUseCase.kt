package com.gmail.shu10.dev.app.domain

import javax.inject.Inject

class SaveDiaryUseCase @Inject constructor(private val repository: IDiaryRepository) {
    suspend operator fun invoke(diary :Diary) {
        repository.saveDiary(diary)
    }
}