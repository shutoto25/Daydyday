package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * 日記取得UseCase
 */
class GetDiaryUseCase @Inject constructor(
    private val repository: IDiaryRepository
) {
    /**
     * 日記取得
     */
    operator fun invoke(dateList: List<String>): Flow<Diary?> = flow {
        dateList.forEach { date ->
            repository.getDiaryByDate(date)
                .filterNotNull().collect { diary ->
                    emit(diary)
                }
        }
    }.flowOn(Dispatchers.IO)
}