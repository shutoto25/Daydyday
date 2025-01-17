package com.gmail.shu10.dev.app.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ホーム画面ViewModel
 */
@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val getDiaryUseCase: GetDiaryUseCase
) : ViewModel() {

    private val _diaryList = MutableStateFlow<List<Diary>>(emptyList())
    val diaryList: StateFlow<List<Diary>> = _diaryList

    init {
        // 日付リスト生成
        val dateList = generateDateList()
        // 初期リストとして日付のデータのみ持ったDiaryを生成して設定
        val initialList = dateList.map { Diary(date = it) }
        _diaryList.value = initialList

        fetchDiaryListData(dateList)
    }

    /**
     * 日記リスト取得
     * ＠param dateList 日付リスト
     */
    private fun fetchDiaryListData(dateList: List<String>) {
        viewModelScope.launch {
            getDiaryUseCase(dateList).collect { diary -> updateDiary(diary) }
        }
    }

    /**
     * 日記リストデータを更新
     * ＠param updateDiary 更新する日記データ
     */
    fun updateDiary(updateDiary: Diary?) {
        if (updateDiary == null) return

        _diaryList.update { currentList ->
            currentList.map { diary ->
                if (diary.date == updateDiary.date) {
                    Log.d("TEST", "updateDiary() update ${updateDiary.date}:$updateDiary")
                    updateDiary
                } else {
                    diary
                }
            }
        }
    }

    /**
     * 今日を起点に過去1年と未来1年の日付リストを生成
     */
    private fun generateDateList(): List<String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // 過去1年分の日付を生成（365日）
        val pastDates = (1..365).map { today.minusDays(it.toLong()).format(formatter) }.reversed()

        // 未来1年分の日付を生成（365日）
        val futureDates = (1..365).map { today.plusDays(it.toLong()).format(formatter) }

        // 今日の日付を中央に配置してリストを統合
        return pastDates + today.format(formatter) + futureDates
    }
}