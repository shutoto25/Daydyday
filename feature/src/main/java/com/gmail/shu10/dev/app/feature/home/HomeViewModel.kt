//package com.gmail.shu10.dev.app.feature.home
//
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//import androidx.compose.material.icons.filled.KeyboardArrowUp
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.gmail.shu10.dev.app.domain.Diary
//import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.time.LocalDate
//import java.time.format.DateTimeFormatter
//import javax.inject.Inject
//
///**
// * ホーム画面ViewModel
// */
//@HiltViewModel
//open class HomeViewModel @Inject constructor(
//    private val getDiaryUseCase: GetDiaryUseCase,
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
//    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
//
//    init {
//        fetchAllDiaries()
//    }
//
//    /**
//     * 全日記データ取得
//     */
//    private fun fetchAllDiaries() {
//        try {
//            viewModelScope.launch {
//                getDiaryUseCase().collect { diaries ->
//                    val allDiaries = generateDateList().map { dateDiary ->
//                        diaries.find { it.date == dateDiary.date } ?: dateDiary
//                    }
//                    _uiState.value = HomeUiState.Success(
//                        diaryList = allDiaries,
//                        isFabVisible = false,
//                        fabIcon = Icons.Default.KeyboardArrowDown,
//                    )
//                }
//            }
//        } catch (e: Exception) {
//            _uiState.value = HomeUiState.Error("エラーが発生しました")
//        }
//    }
//
//    /**
//     * 日記リストデータを更新
//     * ＠param updateDiary 更新する日記データ
//     */
//    fun updateDiaryList(updateDiary: Diary?) {
//        if (updateDiary == null) return
//
//        _uiState.update { currentState ->
//            if (currentState is HomeUiState.Success) {
//                val updatedDiaryList = currentState.diaryList.map {
//                    if (it.date == updateDiary.date) updateDiary else it
//                }
//                currentState.copy(diaryList = updatedDiaryList)
//            } else {
//                currentState
//            }
//        }
//    }
//
//    fun updateFabState(gridPosition: Int) {
//        _uiState.update { currentState ->
//            if (currentState is HomeUiState.Success) {
//                val isFabVisible = gridPosition !in 360..365
//                val newFabIcon =
//                    if (gridPosition <= 365) Icons.Default.KeyboardArrowDown
//                    else Icons.Default.KeyboardArrowUp
//                currentState.copy(isFabVisible = isFabVisible, fabIcon = newFabIcon)
//            } else {
//                currentState
//            }
//        }
//    }
//
//    /**
//     * 今日を起点に過去1年と未来1年の日付リストを生成
//     */
//    private fun generateDateList(): List<Diary> {
//        val today = LocalDate.now()
//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//
//        // 過去1年分の日付を生成（365日）
//        val pastDates =
//            (1..365).map { today.minusDays(it.toLong()).format(formatter) }.reversed()
//
//        // 未来1年分の日付を生成（365日）
//        val futureDates = (1..365).map { today.plusDays(it.toLong()).format(formatter) }
//
//        // 今日の日付を中央に配置してリストを統合
//        val dataListString = pastDates + today.format(formatter) + futureDates
//
//        return dataListString.map { date -> Diary(date = date) }
//    }
//}
//
//sealed class HomeUiState {
//    object Loading : HomeUiState()
//
//    data class Success(
//        val diaryList: List<Diary>,
//        val isFabVisible: Boolean,
//        val fabIcon: ImageVector,
//    ) : HomeUiState()
//
//    data class Error(val message: String) : HomeUiState()
//}