package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.vector.ImageVector
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

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    /**
     * UI state
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var _selectedDiary: Diary? = null

    /**
     * 選択中の日記
     */
    var selectedDiary: Diary?
        get() = _selectedDiary
        set(value) {
            _selectedDiary = value
        }

    init {
        syncDiaryList()
    }

    /**
     * 日記リストを同期
     */
    fun syncDiaryList() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                getDiaryUseCase().collect { diaries ->
                    _diaryList.update { currentList ->
                        currentList.map { diary ->
                            diaries.find { it.date == diary.date } ?: diary
                        }
                    }
                    _uiState.value = HomeUiState.Success(
                        diaryList = _diaryList.value,
                        fabIcon = Icons.Default.KeyboardArrowDown,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "UnKnown Error")
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
    fun saveDiaryToLocal(diary: Diary) {
        viewModelScope.launch {
            saveDiaryUseCase(diary)
        }
    }

    /**
     * 今日の日記を選択
     */
    fun setTodayDiary() {
        _selectedDiary = _diaryList.value.find {
            it.date == LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
    }

    /**
     * メディアタイプを設定
     * @param mediaType MediaType
     */
    fun setMediaType(mediaType: MediaType) {
        setConfigUseCase.setMediaType(mediaType.name)
    }

    /**
     * メディアタイプを取得
     * @return MediaType(初回起動時はnull)
     */
    fun getMediaType(): MediaType? {
        val mediaTypeStr = getConfigUseCase.getMediaType()
        Log.d("TEST", "getMediaType() called $mediaTypeStr")
        return if (mediaTypeStr.isBlank()) null else MediaType.valueOf(mediaTypeStr)
    }

    /**
     * 写真保存
     * @param context Context
     * @param uri Uri
     * @param date 日付
     */
    fun savePhotoToAppDir(context: Context, uri: Uri, date: String): File? {

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, "$date.jpg")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        createStillImageVideo(BitmapFactory.decodeFile(file.path), targetFile(context, date))
        return file
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

    private fun targetFile(context: Context, date: String): File {
        val appDir = File(context.filesDir, "videos/1sec")
        if (!appDir.exists()) appDir.mkdirs()
        val targetFile = File(appDir, "$date.mp4")
        return targetFile
    }

    private fun createStillImageVideo(
        bitmap: Bitmap,
        outputFile: File,
        rotationDegrees: Float = 0f,
    ) {
        // 固定出力解像度 1920×1920 を使用するため、ここでは直接 1920,1920 を指定する
        val encoder = ImageToVideoEncoder(outputFile.absolutePath, 1920, 1920, frameRate = 30)
        encoder.encodeStillImage(bitmap, rotationDegrees)
        Log.d("StillImageVideo", "動画生成完了: ${outputFile.absolutePath}")
    }
}


sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(
        val diaryList: List<Diary>,
        val fabIcon: ImageVector,
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

enum class MediaType {
    PHOTO,
    VIDEO,
    PHOTO_AND_VIDEO // 現状使えないが使える様になったら課金対象にする
}