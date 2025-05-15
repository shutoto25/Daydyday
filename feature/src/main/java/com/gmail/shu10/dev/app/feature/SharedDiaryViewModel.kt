package com.gmail.shu10.dev.app.feature

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.GetConfigUseCase
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import com.gmail.shu10.dev.app.domain.SetConfigUseCase
import com.gmail.shu10.dev.app.feature.diarydetail.DiaryDetailUiState
import com.gmail.shu10.dev.app.feature.main.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    init {
        // roomからflowで日記リストを取得&同期
        syncDiaryList()
    }

    /**
     * 日記リストのアイテムを選択
     * @param index 選択した日記のインデックス
     * @param selectedDiary 選択した日記
     */
    fun selectDiaryEvent(index: Int, selectedDiary: Diary) {
        _detailUiState.value = DiaryDetailUiState.Success(
            diaryList = _diaryList.value,
            index = index,
            diary = selectedDiary,
        )
    }

    /**
     * 日記リストのアイテムを更新
     * @param updatedDiary 更新対象日記
     */
    fun updateDiaryListItem(updatedDiary: Diary) {
        val saveData = updatedDiary.copy(uuid = updatedDiary.uuid.ifEmpty {
            UUID.randomUUID().toString() /* 初回保存時 */
        })
        viewModelScope.launch {
            saveDiaryUseCase(saveData)
        }
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
     * 写真を1秒動画に変換して保存
     * @param context Context
     * @param uri Uri
     * @param date 日付
     * @return File? 生成した動画ファイル
     */
    fun save1secFromPhoto(context: Context, uri: Uri, date: String): File? {
        // 動画格納用ディレクトリ作成
        val videoDir = File(context.filesDir, "videos/1sec")
        if (!videoDir.exists()) videoDir.mkdirs()

        // 出力先の動画ファイル
        val targetVideoFile = File(videoDir, "$date.mp4")

        // 画像ディレクトリの作成
        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()

        // 出力先の画像ファイル
        val imageFile = File(appDir, "$date.jpg")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // 非同期処理で画像から動画を生成
            viewModelScope.launch {
                try { } catch (e: Exception) {
                    Log.e(
                        "SharedDiaryViewModel",
                        "静止画から動画への変換中にエラーが発生しました",
                        e
                    )
                }
            }
            return imageFile
        } catch (e: Exception) {
            Log.e("SharedDiaryViewModel", "画像の保存中にエラーが発生しました", e)
            return null
        }
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
     * 動画からサムネイルを取得して保存
     */
    fun saveThumbnails(context: Context, uri: Uri, date: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val result = getVideoThumbnail(context, uri)?.let { bitmap ->
                // 画像ディレクトリの作成
                val appDir = File(context.filesDir, "images")
                if (!appDir.exists()) appDir.mkdirs()

                // 出力先の画像ファイル
                val imageFile = File(appDir, "$date.jpg")

                // ビットマップをJPEGとして保存
                imageFile.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                imageFile
            }

            onComplete(result)
        }
    }

    /**
     * 動画からサムネイルを取得
     * @param context Context
     * @param videoUri 動画のURI
     * @return サムネイル画像
     */
    private suspend fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                retriever.getFrameAtTime(0) // 最初のフレームを取得
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                retriever.release()
            }
        }
    }
}