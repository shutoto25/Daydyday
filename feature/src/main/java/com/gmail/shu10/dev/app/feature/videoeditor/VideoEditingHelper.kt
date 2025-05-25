package com.gmail.shu10.dev.app.feature.videoeditor

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 動画編集ヘルパークラス
 * 動画編集に関する共通的な処理を提供
 */
object VideoEditingHelper {

    /**
     * 動画の出力ディレクトリを作成
     * @param context Context
     * @param subDirectory サブディレクトリ名
     * @return 作成されたディレクトリ
     */
    fun createVideoDirectory(context: Context, subDirectory: String = ""): File {
        val baseDir = File(context.filesDir, "videos")
        val targetDir = if (subDirectory.isNotEmpty()) {
            File(baseDir, subDirectory)
        } else {
            baseDir
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        return targetDir
    }

    /**
     * 画像の出力ディレクトリを作成
     * @param context Context
     * @return 作成されたディレクトリ
     */
    fun createImageDirectory(context: Context): File {
        val imageDir = File(context.filesDir, "images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        return imageDir
    }

    /**
     * 一時ファイルを作成
     * @param context Context
     * @param prefix ファイル名のプレフィックス
     * @param suffix ファイル名のサフィックス
     * @return 作成された一時ファイル
     */
    fun createTempFile(context: Context, prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }

    /**
     * ファイルサイズを取得
     * @param file ファイル
     * @return ファイルサイズ（バイト）
     */
    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }

    /**
     * ファイルサイズを人間が読みやすい形式で取得
     * @param file ファイル
     * @return フォーマットされたファイルサイズ
     */
    fun getFormattedFileSize(file: File): String {
        val size = getFileSize(file)
        return when {
            size >= 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            size >= 1024 -> "${size / 1024} KB"
            else -> "$size bytes"
        }
    }
}