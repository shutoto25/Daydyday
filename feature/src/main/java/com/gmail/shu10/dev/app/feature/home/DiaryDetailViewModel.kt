package com.gmail.shu10.dev.app.feature.home

import ImageToVideoEncoder
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // ※ Activity や Service 内で呼び出す例
    private fun createStillImageVideo(bitmap: Bitmap, outputFile: File) {

        // 3. エンコード実行（例：1280x720, 30fps）
        val encoder = ImageToVideoEncoder(outputFile.absolutePath, bitmap.width, bitmap.height
            , frameRate = 30)
        encoder.encodeStillImage(bitmap)

        Log.d("StillImageVideo", "動画生成完了: ${outputFile.absolutePath}")
    }
}