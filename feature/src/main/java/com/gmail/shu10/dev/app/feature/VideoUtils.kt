package com.gmail.shu10.dev.app.feature


import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.gmail.shu10.dev.app.core.video.SimpleVideoCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 動画関連のユーティリティ
 */
object VideoUtils {

    /**
     * URIから画像を読み込んで1秒動画を作成
     * @param context Context
     * @param imageUri 画像URI
     * @param outputFile 出力ファイル
     * @return 成功可否
     */
    suspend fun createVideoFromImageUri(
        context: Context,
        imageUri: Uri,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 画像を読み込み
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return@withContext false

            // 動画作成
            val creator = SimpleVideoCreator()
            val result = creator.createVideoFromBitmap(bitmap, outputFile)

            bitmap.recycle()
            result
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 画像ファイルから1秒動画を作成
     * @param imageFile 画像ファイル
     * @param outputFile 出力ファイル
     * @return 成功可否
     */
    suspend fun createVideoFromImageFile(
        imageFile: File,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@withContext false

            val creator = SimpleVideoCreator()
            val result = creator.createVideoFromBitmap(bitmap, outputFile)

            bitmap.recycle()
            result
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 動画ファイルが存在するかチェック
     * @param file ファイル
     * @return 存在する場合はtrue
     */
    fun isValidVideoFile(file: File): Boolean {
        return file.exists() && file.length() > 0
    }

    /**
     * 動画ファイルのサイズを取得（MB単位）
     * @param file ファイル
     * @return ファイルサイズ（MB）
     */
    fun getVideoFileSizeMB(file: File): Double {
        return if (file.exists()) {
            file.length() / (1024.0 * 1024.0)
        } else {
            0.0
        }
    }
}