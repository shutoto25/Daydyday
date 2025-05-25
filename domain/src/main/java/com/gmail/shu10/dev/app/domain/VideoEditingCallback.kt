package com.gmail.shu10.dev.app.domain

/**
 * 動画編集エラー種別
 */
enum class VideoEditingError {
    INPUT_FILE_NOT_FOUND,
    OUTPUT_FILE_CREATION_FAILED,
    ENCODING_FAILED,
    DECODING_FAILED,
    INSUFFICIENT_STORAGE,
    INVALID_FORMAT,
    UNKNOWN_ERROR
}

/**
 * 動画編集進捗コールバック
 */
interface VideoEditingCallback {
    /**
     * 進捗更新
     * @param progress 進捗率（0-100）
     */
    fun onProgress(progress: Int)

    /**
     * 処理完了
     * @param success 成功可否
     * @param outputFile 出力ファイル
     */
    fun onComplete(success: Boolean, outputFile: String? = null)

    /**
     * エラー発生
     * @param error エラー種別
     * @param message エラーメッセージ
     */
    fun onError(error: VideoEditingError, message: String)
}

/**
 * 動画編集設定オプション
 */
data class VideoEditingOptions(
    val quality: VideoQuality = VideoQuality.MEDIUM,
    val frameRate: Int = 30,
    val bitRate: Int = 2_000_000,
    val enableProgressCallback: Boolean = true
)

/**
 * 動画品質設定
 */
enum class VideoQuality(val width: Int, val height: Int, val bitRate: Int) {
    LOW(720, 480, 1_000_000),
    MEDIUM(1280, 720, 2_000_000),
    HIGH(1920, 1080, 6_000_000)
}
