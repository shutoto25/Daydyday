package com.gmail.shu10.dev.app.feature.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.GetConfigUseCase
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import com.gmail.shu10.dev.app.domain.SetConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * ホーム画面と詳細画面で共有するViewModel
 */
@HiltViewModel
class SharedDiaryViewModel @Inject constructor(
    private val getDiaryUseCase: GetDiaryUseCase,
    private val saveDiaryUseCase: SaveDiaryUseCase,
    private val setConfigUseCase: SetConfigUseCase,
    private val getConfigUseCase: GetConfigUseCase,
) : ViewModel() {
    private val _diaryList = MutableStateFlow(generateDateList())

    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow<DiaryDetailUiState>(DiaryDetailUiState.Loading)
    val detailUiState: StateFlow<DiaryDetailUiState> = _detailUiState.asStateFlow()

    private val ffmpegProcessor = FFmpegVideoProcessor()

    init {
        // roomからflowで日記リストを取得&同期
        syncDiaryList()
    }

    fun selectDiaryEvent(index: Int, selectedDiary: Diary) {
        _detailUiState.value = DiaryDetailUiState.Success(
            diaryList = _diaryList.value,
            index = index,
            diary = selectedDiary,
        )
    }

    fun updateDiaryListItem(updatedDiary: Diary) {
        val saveData = updatedDiary.copy(uuid = updatedDiary.uuid.ifEmpty {
            UUID.randomUUID().toString() /* 初回保存時 */
        })
        saveDiaryToLocal(saveData)
    }

    /**
     * 日記リストを同期
     */
    fun syncDiaryList() {
        _homeUiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                // DBから日記リストを取得し、その後の変更を検知する
                getDiaryUseCase().collect { diaries ->
                    _diaryList.update { currentList ->
                        currentList.map { diary ->
                            diaries.find { it.date == diary.date } ?: diary
                        }
                    }
                    // ホーム画面のUI状態を更新
                    _homeUiState.value = HomeUiState.Success(diaryList = _diaryList.value)

                    // 詳細画面のUI状態を更新
                    val currentState = _detailUiState.value
                    if (currentState is DiaryDetailUiState.Success) {
                        _detailUiState.value = currentState.copy(diaryList = _diaryList.value)
                    }
                }
            } catch (e: Exception) {
                _homeUiState.value = HomeUiState.Error(e.message ?: "UnKnown Error")
            }
        }
    }

    /**
     * 今日を起点に過去1年と未来1年の日付リストを生成
     */
    private fun generateDateList(): List<Diary> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // 過去1年分の日付を生成（365日）
        val pastDates =
            (1..365).map { today.minusDays(it.toLong()).format(formatter) }.reversed()
        // 未来1年分の日付を生成（365日）
        val futureDates = (1..365).map { today.plusDays(it.toLong()).format(formatter) }
        // 今日の日付を中央に配置してリストを統合
        val dataListString = pastDates + today.format(formatter) + futureDates

        return dataListString.map { date -> Diary(date = date) }
    }

    /**
     * 内部DBへ日記を保存
     * @param diary 日記
     */
    private fun saveDiaryToLocal(diary: Diary) {
        viewModelScope.launch {
            saveDiaryUseCase(diary)
        }
    }

    /**
     * 写真保存（FFmpeg利用版）
     * @param context Context
     * @param uri Uri
     * @param date 日付
     */
    fun savePhotoToAppDir(context: Context, uri: Uri, date: String): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        // 画像ディレクトリの作成
        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()

        // 写真を保存
        val imageFile = File(appDir, "$date.jpg")
        inputStream.use { input ->
            imageFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 静止画から動画を生成（非同期処理）
        viewModelScope.launch {
            try {
                val targetVideoFile = targetFile(context, date)

                // FFmpegを使って静止画から1秒動画を生成
                val success = ffmpegProcessor.createVideoFromImage(
                    context,
                    imageFile,
                    targetVideoFile
                )

                if (success) {
                    Log.d("SharedDiaryViewModel", "静止画から動画への変換が成功しました: $date")
                } else {
                    Log.e("SharedDiaryViewModel", "静止画から動画への変換に失敗しました: $date")
                }
            } catch (e: Exception) {
                Log.e("SharedDiaryViewModel", "静止画から動画への変換中にエラーが発生しました", e)
            }
        }

        return imageFile
    }

    /**
     * 動画保存
     */
    fun saveVideoToAppDir(context: Context, uri: Uri, date: String): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val appDir = File(context.filesDir, "videos")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, "$date.mp4")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    /**
     * 動画出力用のターゲットファイルを取得
     */
    private fun targetFile(context: Context, date: String): File {
        val appDir = File(context.filesDir, "videos/1sec")
        if (!appDir.exists()) appDir.mkdirs()
        return File(appDir, "$date.mp4")
    }
}

/**
 * ホーム画面のUI状態
 */
sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(
        val diaryList: List<Diary>,
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

/**
 * 日記詳細画面のUI状態
 */
sealed class DiaryDetailUiState {
    object Loading : DiaryDetailUiState()

    data class Success(
        val diaryList: List<Diary>,
        val index: Int,
        val diary: Diary?,
    ) : DiaryDetailUiState()

    data class Error(val message: String) : DiaryDetailUiState()
}