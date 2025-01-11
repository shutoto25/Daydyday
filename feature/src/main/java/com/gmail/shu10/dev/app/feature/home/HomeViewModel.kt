package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 *
 */
@HiltViewModel
open class HomeViewModel @Inject constructor() : ViewModel() {

    private val _dateList = MutableStateFlow(generateDateList())
    open val dateList: StateFlow<List<String>> = _dateList

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