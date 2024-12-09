package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.inject.Inject

/**
 *
 */
@HiltViewModel
open class HomeViewModel @Inject constructor() : ViewModel() {

    companion object {
        const val GENERATE_LIST_COUNT = 20
    }

    private val _dateList = MutableStateFlow(generateFeatureDateList())
    open val dateList: StateFlow<List<String>> = _dateList

    /**
     * アイテムを追加で20件読み込む
     */
    fun loadMoreDateList(direction: ScrollDirection) {
        viewModelScope.launch {
            // 追加で20件アイテムを読み込む
            val newItems =
                when (direction) {
                    ScrollDirection.UP -> generatePastDateList(_dateList.value.first())
                    ScrollDirection.DOWN -> generateFeatureDateList(_dateList.value.last())
                }
            when (direction) {
                ScrollDirection.UP -> _dateList.update { newItems + it }
                ScrollDirection.DOWN -> _dateList.update { it + newItems }
            }
            // 読み込むたびにアイテムが追加されてメモリに保持され続けてしまうのでメモリ効率向上のために最大サイズを制限する
//            _dateList.value = when (direction) {
//                ScrollDirection.UP -> if (updateList.size > 100) updateList.take(100) else updateList
//                ScrollDirection.DOWN -> if (updateList.size > 100) updateList.takeLast(100) else updateList
//            }
            // メモ：表示に使用するLazyColumnは、スクロールごとに表示されるアイテムを適宜再利用し、
            // 表示するアイテムのみをメモリに保持するのでUI表示の面では効率的
        }
    }

    /**
     * 未来の日付リストを生成
     * @param startDate 読み込み開始日
     */
    private fun generateFeatureDateList(@Nullable startDate: String? = null): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        // 開始日指定がある場合
        startDate?.let {
            calendar.time = dateFormat.parse(it)!!
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return List(GENERATE_LIST_COUNT) {
            dateFormat.format(calendar.time).also {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    /**
     * 過去の日付リストを生成
     * @param startDate 読み込み開始日
     */
    private fun generatePastDateList(@Nonnull startDate: String): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            time = dateFormat.parse(startDate)!!
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return List(GENERATE_LIST_COUNT) {
            dateFormat.format(calendar.time).also {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }.reversed() // 過去の日付は逆順で生成されるため、再度逆順にして追加
    }
}

/**
 * スクロール向き
 */
enum class ScrollDirection {
    UP,
    DOWN
}