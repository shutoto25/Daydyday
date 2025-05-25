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
import com.gmail.shu10.dev.app.domain.CreateVideoFromImagesUseCase
import com.gmail.shu10.dev.app.domain.MergeVideosUseCase
import com.gmail.shu10.dev.app.domain.TrimVideoUseCase
import com.google.common.collect.ImmutableList

/**
 * 動画編集画面のViewModel
 */
@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    private val trimVideoUseCase: TrimVideoUseCase,
    private val createVideoFromImagesUseCase: CreateVideoFromImagesUseCase,
    private val mergeVideosUseCase: MergeVideosUseCase
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

    /**
     * 動画をトリミング（UseCase使用版）
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

                val startTimeUs = startMs * 1000L // ミリ秒をマイクロ秒に変換
                val durationUs = 1_000_000L // 1秒固定

                val result = trimVideoUseCase(
                    context = context,
                    inputUri = inputUri,
                    outputFile = outputFile,
                    startTimeUs = startTimeUs,
                    durationUs = durationUs
                )

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    if (result) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "動画トリミングエラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onError()
                }
            }
        }
    }

    /**
     * 画像から動画を生成
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

                val result = createVideoFromImagesUseCase(
                    context = context,
                    imagePaths = imagePaths,
                    outputFile = outputFile,
                    frameRate = 30,
                    durationPerImageMs = 2000L
                )

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    if (result) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "画像から動画生成エラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onError()
                }
            }
        }
    }

    /**
     * 複数動画を結合
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

                val result = mergeVideosUseCase(
                    context = context,
                    inputUris = inputUris,
                    outputFile = outputFile
                )

                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    if (result) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "動画結合エラー", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onError()
                }
            }
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
                    val frame = retriever.getFrameAtTime(
                        i * interval * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    frame?.let {
                        cropToAspectRatio(it, videoHeight, videoWidth, rotation)
                    }
                }.also {
                    retriever.release()
                }

                _thumbnails.value = ImmutableList.copyOf(thumbnails)
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