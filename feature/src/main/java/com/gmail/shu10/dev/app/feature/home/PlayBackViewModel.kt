package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor() : ViewModel() {

    private val ffmpegProcessor = FFmpegVideoProcessor()

    // 結合後の動画 URI を保持する状態
    private val _mergedVideoUri = mutableStateOf<Uri?>(null)
    val mergedVideoUri: State<Uri?> get() = _mergedVideoUri

    // 処理中フラグ
    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> get() = _isProcessing

    /**
     * 1秒間の動画ファイルを連結して、ひとつの動画ファイルとして出力する。
     * 二重実行を防止
     */
    fun mergeVideos(context: Context) {
        // 処理中なら早期リターン
        if (_isProcessing.value) {
            Log.d("VideoMerge", "すでに処理中です")
            return
        }

        _isProcessing.value = true
        _mergedVideoUri.value = null  // リセット

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 入力ディレクトリから動画ファイル一覧を取得
                val videoDir = File(context.filesDir, "videos/1sec")
                val videoFiles = videoDir.listFiles { file ->
                    file.extension.equals("mp4", ignoreCase = true)
                }?.sortedBy { it.name } ?: run {
                    Log.e("VideoConcat", "動画ファイルが見つかりません")
                    _isProcessing.value = false
                    return@launch
                }

                if (videoFiles.isEmpty()) {
                    Log.e("VideoConcat", "動画ファイルがありません")
                    _isProcessing.value = false
                    return@launch
                }

                // 出力先ファイル
                val outputFile = File(context.filesDir, "videos/merged.mp4")

                // FFmpegを使用して動画を結合
                val success = ffmpegProcessor.concatenateVideos(
                    context,
                    videoFiles,
                    outputFile
                )

                withContext(Dispatchers.Main) {
                    if (success && outputFile.exists()) {
                        _mergedVideoUri.value = outputFile.toUri()
                        Log.d("VideoMerge", "動画連結完了: ${outputFile.absolutePath}")
                    } else {
                        Log.e("VideoMerge", "動画連結に失敗しました")
                    }
                    _isProcessing.value = false
                }
            } catch (e: Exception) {
                Log.e("VideoMerge", "動画連結中にエラー発生", e)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
            }
        }
    }
}