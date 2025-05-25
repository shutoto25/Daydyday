package com.gmail.shu10.dev.app.feature

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.CreateVideoFromImagesUseCase
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
    private val createVideoFromImagesUseCase: CreateVideoFromImagesUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "SharedDiaryViewModel"
    }

    private val _diaryList = MutableStateFlow(generateDateList())

    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow<DiaryDetailUiState>(DiaryDetailUiState.Loading)
    val detailUiState: StateFlow<DiaryDetailUiState> = _detailUiState.asStateFlow()

    init {
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
            UUID.randomUUID().toString()
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
                getDiaryUseCase().collect { diaries ->
                    _diaryList.update { currentList ->
                        currentList.map { diary ->
                            diaries.find { it.date == diary.date } ?: diary
                        }
                    }
                    _homeUiState.value = HomeUiState.Success(diaryList = _diaryList.value)

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
     * 写真を1秒動画に変換して保存（更新版）
     * @param context Context
     * @param uri Uri
     * @param date 日付
     * @return File? 生成した画像ファイル
     */
    fun save1secFromPhoto(context: Context, uri: Uri, date: String): File? {
        // 画像ディレクトリの作成
        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()

        // 出力先の画像ファイル
        val imageFile = File(appDir, "$date.jpg")

        try {
            // 画像を保存
            context.contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // 非同期で画像から1秒動画を生成
            viewModelScope.launch {
                try {
                    val videoDir = File(context.filesDir, "videos/1sec")
                    if (!videoDir.exists()) videoDir.mkdirs()
                    val videoFile = File(videoDir, "$date.mp4")

                    val success = createVideoFromImagesUseCase(
                        context = context,
                        imagePaths = listOf(imageFile.absolutePath),
                        outputFile = videoFile,
                        frameRate = 30,
                        durationPerImageMs = 1000L // 1秒
                    )

                    if (success) {
                        Log.d(TAG, "画像から1秒動画の生成完了: ${videoFile.absolutePath}")
                    } else {
                        Log.e(TAG, "画像から1秒動画の生成に失敗")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "画像から動画の変換中にエラーが発生", e)
                }
            }

            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "画像の保存中にエラーが発生", e)
            return null
        }
    }

    /**
     * 動画保存
     * @param context Context
     * @param uri Uri
     * @param date 日付
     * @return File? 保存した動画ファイル
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
     * @param context Context
     * @param uri Uri
     * @param date 日付
     * @param onComplete 完了コールバック
     */
    fun saveThumbnails(context: Context, uri: Uri, date: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val result = getVideoThumbnail(context, uri)?.let { bitmap ->
                val appDir = File(context.filesDir, "images")
                if (!appDir.exists()) appDir.mkdirs()
                val imageFile = File(appDir, "$date.jpg")

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
                retriever.getFrameAtTime(0)
            } catch (e: Exception) {
                Log.e(TAG, "サムネイル取得エラー", e)
                null
            } finally {
                retriever.release()
            }
        }
    }

    /**
     * 今日を起点に過去1年と未来1年の日付リストを生成
     * @return 日記データリスト
     */
    private fun generateDateList(): List<Diary> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val pastDates = (1..365).map {
            today.minusDays(it.toLong()).format(formatter)
        }.reversed()

        val futureDates = (1..365).map {
            today.plusDays(it.toLong()).format(formatter)
        }

        val dataListString = pastDates + today.format(formatter) + futureDates

        return dataListString.map { date -> Diary(date = date) }
    }

    /**
     * 複数の動画を結合して1つの動画を作成
     * @param context Context
     * @param videoUris 動画URIリスト
     * @param outputFileName 出力ファイル名
     * @param onComplete 完了コールバック
     */
    fun mergeVideosToSingleFile(
        context: Context,
        videoUris: List<Uri>,
        outputFileName: String,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val outputDir = File(context.filesDir, "videos/merged")
                if (!outputDir.exists()) outputDir.mkdirs()
                val outputFile = File(outputDir, "$outputFileName.mp4")

                // 既存のPlayBackViewModelのmergeVideos機能を使用するか
                // 新しいMergeVideosUseCaseを使用してもよい

                withContext(Dispatchers.Main) {
                    onComplete(outputFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "動画結合エラー", e)
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    /**
     * 写真から1秒動画を作成（将来実装版）
     */
    fun save1secFromPhotoFuture(context: Context, uri: Uri, date: String): File? {
        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()
        val imageFile = File(appDir, "$date.jpg")

        try {
            // 画像を保存
            context.contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // 非同期で動画作成
            viewModelScope.launch {
                try {
                    val videoDir = File(context.filesDir, "videos/1sec")
                    if (!videoDir.exists()) videoDir.mkdirs()
                    val videoFile = File(videoDir, "$date.mp4")

                    val success = VideoUtils.createVideoFromImageFile(imageFile, videoFile)

                    if (success) {
                        Log.d(TAG, "1秒動画作成完了: ${videoFile.absolutePath}")
                    } else {
                        Log.e(TAG, "1秒動画作成失敗")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "動画作成エラー", e)
                }
            }

            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "画像保存エラー", e)
            return null
        }
    }
}