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
import com.gmail.shu10.dev.app.feature.main.FFmpegVideoProcessor
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
import com.google.common.collect.ImmutableList

/**
 * 動画編集画面のViewModel
 */
@HiltViewModel
class VideoEditorViewModel @Inject constructor() : ViewModel() {

    private val ffmpegProcessor = FFmpegVideoProcessor()

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

    /**
     * 動画からサムネイル（フレーム）を切り出す
     * @param context context
     * @param videoUri 動画URL
     * @return サムネイル（フレーム画像）リスト
     */
    fun extractThumbnails(context: Context, videoUri: Uri?) {
        viewModelScope.launch {
            videoUri?.let {
                // フレームとメタデータを取得するクラス
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)

                // 幅・高さ情報
                val videoWidth = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0
                val videoHeight = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0

                // 回転情報
                val rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                )?.toIntOrNull() ?: 0

                // 再生時間 (ミリ秒単位) 情報
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                Log.d(
                    "VideoEditorViewModel",
                    "extractThumbnails() called:" +
                            "Width: $videoWidth, Height: $videoHeight, " +
                            "Rotation:$rotation, DurationMs: $durationMs"
                )

                // 0.5秒単位でサムネイルを取得
                val interval = 500L
                val count = (durationMs / interval).toInt()

                val thumbnails = (0 until count).mapNotNull { i ->
                    // ミリ秒変換して、指定した時間に一番近いフレームを取得
                    val frame =
                        retriever.getFrameAtTime(i * interval * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    frame?.let {
                        cropToAspectRatio(it, videoHeight, videoWidth, rotation)
                    }
                }.also {
                    // MediaMetadataRetrieverのメモリリソースを解放
                    retriever.release()
                }

                _thumbnails.value = ImmutableList.copyOf(thumbnails)
            }
        }
    }

    /**
     * アスペクト比にクロップ
     *
     * 動画のメタデータに回転角度という属性が含まれている場合があり、
     * 例えば、スマートフォンで縦長動画を撮影した際にカメラは動画を横向き（Landscape）として保存しますが、
     * 縦長で撮影したことを示すために「回転情報 = 90度」を付与している可能性がある。
     * その場合はwidthとheightを入れ替えて使用する必要がある。
     *
     * @param bitmap ビットマップ画像
     * @param videoWidth 幅
     * @param videoHeight 高さ
     * @param rotation 回転情報
     * @return 加工済みビットマップ画像
     */
    private fun cropToAspectRatio(
        bitmap: Bitmap, videoWidth: Int, videoHeight: Int, rotation: Int,
    ): Bitmap {
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap.scale(
            if (rotation == 0) videoHeight else videoWidth,
            if (rotation == 0) videoWidth else videoHeight
        )
    }

    /**
     * 動画をトリミングする（FFmpegを使用）
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
                val success = ffmpegProcessor.trimVideoToOneSecond(
                    context,
                    inputUri,
                    outputFile,
                    startMs
                )

                if (success) {
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) { onError() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError() }
            }
        }
    }

    /**
     * 1秒動画のための出力ファイルを取得
     */
    fun targetFile(context: Context, date: String): File {
        val appDir = File(context.filesDir, "videos/1sec")
        if (!appDir.exists()) appDir.mkdirs()
        val targetFile = File(appDir, "$date.mp4")
        return targetFile
    }

    /**
     * FFmpegを使った動画トリミング
     */
    fun startReEncoding(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        // trimVideoメソッドを再利用
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