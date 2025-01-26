package com.gmail.shu10.dev.app.feature.home

import ImageToVideoEncoder
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 日記詳細画面のViewModel
 */
@HiltViewModel
class DiaryDetailViewModel @Inject constructor(
    private val saveDiaryUseCase: SaveDiaryUseCase,
) : ViewModel() {

    /**
     * 日記保存
     * @param diary 日記
     */
    fun saveDiary(diary: Diary) {
        viewModelScope.launch {
            saveDiaryUseCase(diary)
        }
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
        createVideoFromSavedImage(context, BitmapFactory.decodeFile(file.path), targetFile(context, date))
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

    private fun createVideoFromSavedImage(context: Context, bitmap: Bitmap, outputFile: File) {
        Log.d("TEST", "createVideoFromSavedImage() called with: context = $context, bitmap = $bitmap, outputFile = $outputFile")

        // **一時的なMP4ファイルを作成**
        val tempMp4File = File(context.cacheDir, "temp_video.mp4")

        try {
            // **ImageToVideoEncoder を使用して動画を生成**
            val encoder = ImageToVideoEncoder(tempMp4File, bitmap)
            encoder.encodeBitmapToMp4()

            // **エンコードが完了したら、ファイルを移動**
            if (tempMp4File.exists()) {
                tempMp4File.copyTo(outputFile, overwrite = true)
                tempMp4File.delete()
            } else {
                Log.e("TEST", "エンコード失敗: tempMp4File が存在しない")
            }
        } catch (e: Exception) {
            Log.e("TEST", "エンコード中にエラーが発生", e)
        }
    }
}