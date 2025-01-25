package com.gmail.shu10.dev.app.feature.home

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

    @OptIn(UnstableApi::class)
    fun createVideoFromSavedImage(context: Context, bitmap: Bitmap, outputFile: File) {
        Log.d(
            "TEST",
            "createVideoFromSavedImage() called with: context = $context, bitmap = $bitmap, outputFile = $outputFile"
        )
        val tempMp4File = File(context.cacheDir, "temp_video.mp4")
        val encoder = ImageToVideoEncoder(tempMp4File)
        encoder.encodeBitmapToMp4(bitmap)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(tempMp4File))
            .build()


        // ❸ `Transformer` の設定（動画変換用）
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Log.d("TEST", "onCompleted() exportResult = $exportResult")
                    tempMp4File.delete()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    Log.d("TEST", "onError() exportException = ${exportException.message}")
                    tempMp4File.delete()
                }
            })
            .build()

        // ❹ 動画の変換処理を開始
        transformer.start(mediaItem, outputFile.absolutePath) // `absolutePath` を使用
    }
}