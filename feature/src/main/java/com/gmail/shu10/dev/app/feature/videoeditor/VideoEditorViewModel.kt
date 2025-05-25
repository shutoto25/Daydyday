package com.gmail.shu10.dev.app.feature.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.gmail.shu10.dev.app.data.VideoEditorRepositoryImpl
import com.gmail.shu10.dev.app.domain.CreateVideoFromImagesUseCase
import com.gmail.shu10.dev.app.domain.MergeVideosUseCase
import com.gmail.shu10.dev.app.domain.TrimVideoUseCase
import com.gmail.shu10.dev.app.domain.VideoEditingCallback
import com.gmail.shu10.dev.app.domain.VideoEditingError
import com.gmail.shu10.dev.app.domain.VideoEditingOptions
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.core.graphics.scale

/**
 * 動画編集画面のViewModel（改善版）
 */
@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    private val trimVideoUseCase: TrimVideoUseCase,
    private val createVideoFromImagesUseCase: CreateVideoFromImagesUseCase,
    private val mergeVideosUseCase: MergeVideosUseCase,
    private val videoEditorRepository: VideoEditorRepositoryImpl
) : ViewModel() {

    companion object {
        private const val TAG = "VideoEditorViewModel"
    }

    // 動画再生プレイヤー
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    // サムネイルリスト
    private val _thumbnails = MutableStateFlow<ImmutableList<Bitmap>>(ImmutableList.of())
    val thumbnails: StateFlow<ImmutableList<Bitmap>> = _thumbnails.asStateFlow()

    // 動画再生位置
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    // 処理中フラグ
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // 進捗率（0-100）
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // エラーメッセージ
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 動画編集オプション
    private val _videoEditingOptions = MutableStateFlow(VideoEditingOptions())
    val videoEditingOptions: StateFlow<VideoEditingOptions> = _videoEditingOptions.asStateFlow()

    /**
     * 動画をトリミング（改善版）
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startMs 開始時間（ミリ秒）
     * @param onSuccess 成功コールバック
     * @param onError エラーコールバック
     */
    fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progress.value = 0
                _errorMessage.value = null

                val startTimeUs = startMs * 1000L
                val durationUs = 1_000_000L

                val callback = createVideoEditingCallback(onSuccess, onError)

                val result = videoEditorRepository.trimVideoWithCallback(
                    context = context,
                    inputUri = inputUri,
                    outputFile = outputFile,
                    startTimeUs = startTimeUs,
                    durationUs = durationUs,
                    callback = callback
                )

                if (!result) {
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                        _errorMessage.value = "動画トリミングに失敗しました"
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "動画トリミングエラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    _errorMessage.value = "動画トリミング中にエラーが発生しました: ${e.message}"
                    onError()
                }
            }
        }
    }

    /**
     * 画像から動画を生成（改善版）
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param onSuccess 成功コールバック
     * @param onError エラーコールバック
     */
    fun createVideoFromImages(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progress.value = 0
                _errorMessage.value = null

                val callback = createVideoEditingCallback(onSuccess, onError)
                val options = _videoEditingOptions.value

                val result = videoEditorRepository.createVideoFromImagesWithCallback(
                    context = context,
                    imagePaths = imagePaths,
                    outputFile = outputFile,
                    frameRate = 30,
                    durationPerImageMs = 2000L,
                    options = options,
                    callback = callback
                )

                if (!result) {
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                        _errorMessage.value = "画像から動画生成に失敗しました"
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "画像から動画生成エラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    _errorMessage.value = "画像から動画生成中にエラーが発生しました: ${e.message}"
                    onError()
                }
            }
        }
    }

    /**
     * 複数動画を結合（改善版）
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @param onSuccess 成功コールバック
     * @param onError エラーコールバック
     */
    fun mergeVideos(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progress.value = 0
                _errorMessage.value = null

                val callback = createVideoEditingCallback(onSuccess, onError)

                val result = videoEditorRepository.mergeVideosWithCallback(
                    context = context,
                    inputUris = inputUris,
                    outputFile = outputFile,
                    callback = callback
                )

                if (!result) {
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                        _errorMessage.value = "動画結合に失敗しました"
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "動画結合エラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    _errorMessage.value = "動画結合中にエラーが発生しました: ${e.message}"
                    onError()
                }
            }
        }
    }

    /**
     * 動画編集オプションを更新
     * @param options 新しいオプション
     */
    fun updateVideoEditingOptions(options: VideoEditingOptions) {
        _videoEditingOptions.value = options
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * VideoEditingCallbackを作成
     * @param onSuccess 成功コールバック
     * @param onError エラーコールバック
     * @return VideoEditingCallback
     */
    private fun createVideoEditingCallback(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ): VideoEditingCallback {
        return object : VideoEditingCallback {
            override fun onProgress(progress: Int) {
                viewModelScope.launch(Dispatchers.Main) {
                    _progress.value = progress
                }
            }

            override fun onComplete(success: Boolean, outputFile: String?) {
                viewModelScope.launch(Dispatchers.Main) {
                    _isProcessing.value = false
                    _progress.value = if (success) 100 else 0
                    
                    if (success) {
                        Log.d(TAG, "動画編集完了: $outputFile")
                        onSuccess()
                    } else {
                        _errorMessage.value = "動画編集に失敗しました"
                        onError()
                    }
                }
            }

            override fun onError(error: VideoEditingError, message: String) {
                viewModelScope.launch(Dispatchers.Main) {
                    _isProcessing.value = false
                    _progress.value = 0
                    _errorMessage.value = getErrorMessage(error, message)
                    Log.e(TAG, "動画編集エラー: $error - $message")
                    onError()
                }
            }
        }
    }

    /**
     * エラータイプに応じたユーザーフレンドリーなメッセージを取得
     * @param error エラータイプ
     * @param originalMessage 元のエラーメッセージ
     * @return ユーザー向けエラーメッセージ
     */
    private fun getErrorMessage(error: VideoEditingError, originalMessage: String): String {
        return when (error) {
            VideoEditingError.INPUT_FILE_NOT_FOUND -> "入力ファイルが見つかりません。ファイルを確認してください。"
            VideoEditingError.OUTPUT_FILE_CREATION_FAILED -> "出力ファイルの作成に失敗しました。ストレージ容量を確認してください。"
            VideoEditingError.ENCODING_FAILED -> "動画エンコードに失敗しました。設定を変更して再試行してください。"
            VideoEditingError.DECODING_FAILED -> "動画デコードに失敗しました。ファイル形式を確認してください。"
            VideoEditingError.INSUFFICIENT_STORAGE -> "ストレージ容量が不足しています。容量を確保してください。"
            VideoEditingError.INVALID_FORMAT -> "サポートされていないファイル形式です。"
            VideoEditingError.UNKNOWN_ERROR -> "予期しないエラーが発生しました: $originalMessage"
        }
    }

    /**
     * 動画からサムネイル（フレーム）を切り出す
     * @param context context
     * @param videoUri 動画URL
     */
    fun extractThumbnails(context: Context, videoUri: Uri?) {
        viewModelScope.launch {
            videoUri?.let {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, videoUri)

                    val videoWidth = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0
                    val videoHeight = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0
                    val rotation = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toIntOrNull() ?: 0
                    val durationMs = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L

                    Log.d(TAG, "動画情報 - 幅: $videoWidth, 高さ: $videoHeight, 回転: $rotation, 長さ: $durationMs ms")

                    val interval = 500L // 0.5秒間隔
                    val count = (durationMs / interval).toInt()

                    val thumbnails = (0 until count).mapNotNull { i ->
                        try {
                            val frame = retriever.getFrameAtTime(
                                i * interval * 1000,
                                MediaMetadataRetriever.OPTION_CLOSEST
                            )
                            frame?.let {
                                cropToAspectRatio(it, videoHeight, videoWidth, rotation)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "サムネイル取得エラー (フレーム $i)", e)
                            null
                        }
                    }.also {
                        retriever.release()
                    }

                    _thumbnails.value = ImmutableList.copyOf(thumbnails)
                } catch (e: Exception) {
                    Log.e(TAG, "サムネイル抽出エラー", e)
                    _errorMessage.value = "サムネイル抽出に失敗しました: ${e.message}"
                }
            }
        }
    }

    /**
     * アスペクト比にクロップ
     * @param bitmap ビットマップ画像
     * @param videoWidth 幅
     * @param videoHeight 高さ
     * @param rotation 回転情報
     * @return 加工済みビットマップ画像
     */
    private fun cropToAspectRatio(
        bitmap: Bitmap,
        videoWidth: Int,
        videoHeight: Int,
        rotation: Int,
    ): Bitmap {
        val rotatedBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        return rotatedBitmap.scale(
            if (rotation == 0) videoHeight else videoWidth,
            if (rotation == 0) videoWidth else videoHeight
        )
    }

    /**
     * 1秒動画のための出力ファイルを取得
     * @param context Context
     * @param date 日付
     * @return 出力ファイル
     */
    fun targetFile(context: Context, date: String): File {
        val appDir = File(context.filesDir, "videos/1sec")
        if (!appDir.exists()) appDir.mkdirs()
        return File(appDir, "$date.mp4")
    }

    /**
     * 動画トリミング開始（既存メソッド名に合わせる）
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startMs 開始時間（ミリ秒）
     * @param onSuccess 成功コールバック
     * @param onError エラーコールバック
     */
    fun startReEncoding(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        trimVideo(context, inputUri, outputFile, startMs, onSuccess, onError)
    }

    /**
     * ExoPlayerの初期化
     * @param context Context
     */
    fun initializePlayer(context: Context) {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(context).build()
        }
    }

    /**
     * 動画の再生位置を更新
     * @param newPosition 新しい再生位置
     */
    fun updatePosition(newPosition: Long) {
        _position.value = newPosition
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
