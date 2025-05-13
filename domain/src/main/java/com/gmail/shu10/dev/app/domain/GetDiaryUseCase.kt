package com.gmail.shu10.dev.app.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 日記取得UseCase
 * 日記データの取得に関するビジネスロジックを実装する
 */
class GetDiaryUseCase @Inject constructor(
    private val repository: IDiaryRepository
) {
    /**
     * 全日記データ取得
     * @return 日記データのリストをFlowで返す
     */
    operator fun invoke(): Flow<List<Diary>> {
        return repository.getAllDiaries()
    }

    /**
     * 日付から日記データ取得
     * @param date 取得したい日記の日付（yyyy-MM-dd形式）
     * @return 指定された日付の日記データをFlowで返す
     */
    operator fun invoke(date: String): Flow<Diary> {
        return repository.getDiaryByDate(date)
    }
}