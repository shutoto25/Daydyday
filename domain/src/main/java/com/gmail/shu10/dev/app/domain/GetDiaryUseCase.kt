package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiaryUseCase @Inject constructor(private val repository: IDiaryRepository) {
    suspend operator fun invoke(id: String): Flow<Diary?> {
        return repository.getDiary(id)
    }
}