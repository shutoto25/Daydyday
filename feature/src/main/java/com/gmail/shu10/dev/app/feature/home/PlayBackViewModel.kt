package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor(
) : ViewModel() {

    /**
     * 動画ファイル取得
     * @param context Context
     */
    fun getVideoFiles(context: Context): List<File> {
        val videoDir = File(context.filesDir, "videos/trim")
        return videoDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }

    /**
     * 画像ファイル取得
     * @param context Context
     */
    fun getImageFiles(context: Context): List<File> {
        val imageDir = File(context.filesDir, "images")
        return imageDir.listFiles()?.filter { it.extension == "jpg" || it.extension == "png" }
            ?: emptyList()
    }


}
