package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 日記取得UseCase
 */
class GetDiaryUseCase @Inject constructor(
    private val repository: IDiaryRepository
) {
    /**
     * 全日記データ取得
     */
    operator fun invoke(): Flow<List<Diary>> {
        return repository.getAllDiaries()
    }

    /**
     * 日付から日記データ取得
     */
    operator fun invoke(date: String): Flow<Diary> {
        return repository.getDiaryByDate(date)
    }
}