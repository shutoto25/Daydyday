package com.gmail.shu10.dev.app.feature.playback

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
        // synchronized ブロックで排他制御
        synchronized(this) {
            if (_isProcessing.value) {
                Log.d("VideoMerge", "すでに処理中です")
                return
            }
            _isProcessing.value = true
        }

        _mergedVideoUri.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 入力ディレクトリから動画ファイル一覧を取得
                val videoDir = File(context.filesDir, "videos/1sec")
                if (!videoDir.exists() || !videoDir.isDirectory) {
                    Log.e("VideoMerge", "動画ディレクトリが存在しません: ${videoDir.absolutePath}")
                    withContext(Dispatchers.Main) { _isProcessing.value = false }
                    return@launch
                }

                val videoFiles = videoDir.listFiles { file ->
                    file.isFile && file.extension.equals("mp4", ignoreCase = true)
                }?.sortedBy { it.name } ?: emptyList()

                if (videoFiles.isEmpty()) {
                    Log.e("VideoMerge", "動画ファイルがありません")
                    withContext(Dispatchers.Main) { _isProcessing.value = false }
                    return@launch
                }

                // 出力先ファイル
                val outputFile = File(context.filesDir, "videos/merged.mp4")

                Log.d("VideoMerge", "動画結合開始: ${videoFiles.size}ファイル")

                withContext(Dispatchers.Main) {
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