package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * FFmpegを利用した動画処理ユーティリティクラス
 */
class FFmpegVideoProcessor {

    // AtomicBooleanで処理フラグを管理
    private val isProcessing = AtomicBoolean(false)

    // 処理開始前に呼び出し、既に処理中ならfalseを返す
    private fun startProcessingIfNotBusy(): Boolean {
        return isProcessing.compareAndSet(false, true)
    }

    // 処理完了時に呼び出す
    private fun finishProcessing() {
        isProcessing.set(false)
    }
    /**
     * 動画を1秒間にトリミングする
     * @param context コンテキスト
     * @param inputUri 入力動画のURI
     * @param outputFile 出力ファイル
     * @param startMs 開始位置（ミリ秒）
     * @return 処理が成功したかどうか
     */
    suspend fun trimVideoToOneSecond(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 入力パスの取得（コンテンツURIをFFmpegで扱える形式に変換）
            val inputPath = FFmpegKitConfig.getSafParameterForRead(context, inputUri)

            // 出力先の準備
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            // 1秒間の動画を切り出すFFmpegコマンド
            val startSeconds = startMs / 1000f
            val command = "-y -i $inputPath -ss $startSeconds -t 1.0 -c:v mpeg4 ${outputFile.absolutePath}"

            Log.d("FFmpegVideoProcessor", "トリミングコマンド: $command")

            // FFmpegコマンドを実行し、結果を待機
            return@withContext executeFFmpegCommand(command)
        } catch (e: Exception) {
            Log.e("FFmpegVideoProcessor", "トリミング中にエラーが発生しました", e)
            return@withContext false
        }
    }

    /**
     * 画像から1秒間の動画を生成する
     * @param context コンテキスト
     * @param imageFile 入力画像ファイル
     * @param outputFile 出力動画ファイル
     * @return 処理が成功したかどうか
     */
    suspend fun createVideoFromImage(
        context: Context,
        imageFile: File,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 出力先の準備
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            // 静止画→動画の最もシンプルなコマンド（最小限のオプション）
            val command = "-y -loop 1 -i ${imageFile.absolutePath} -t 1 -c:v mpeg4 ${outputFile.absolutePath}"

            Log.d("FFmpegVideoProcessor", "画像→動画変換コマンド: $command")

            // FFmpegコマンドを実行し、結果を待機
            return@withContext executeFFmpegCommand(command)
        } catch (e: Exception) {
            Log.e("FFmpegVideoProcessor", "画像→動画変換中にエラーが発生しました", e)
            return@withContext false
        }
    }

    /**
     * 複数の動画ファイルを結合する
     * @param context コンテキスト
     * @param inputFiles 結合する動画ファイルのリスト（順序通りに結合される）
     * @param outputFile 出力ファイル
     * @return 処理が成功したかどうか
     */
    suspend fun concatenateVideos(
        context: Context,
        inputFiles: List<File>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (inputFiles.isEmpty()) {
                Log.e("FFmpegVideoProcessor", "結合する動画ファイルがありません")
                return@withContext false
            }

            // 出力先の準備
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            // 入力ファイルリストの作成
            val listFile = File(context.cacheDir, "input_list.txt")
            if (listFile.exists()) {
                listFile.delete()
            }

            // ファイルリストの作成
            listFile.printWriter().use { writer ->
                inputFiles.forEach { file ->
                    writer.println("file '${file.absolutePath}'")
                }
            }

            // 動画を結合するFFmpegコマンド
            val command = "-y -f concat -safe 0 -i ${listFile.absolutePath} -c copy ${outputFile.absolutePath}"
            Log.d("FFmpegVideoProcessor", "結合コマンド: $command")

            // 処理完了時に必ずフラグをリセット
            val result = executeFFmpegCommand(command)
            finishProcessing()
            return@withContext result
        } catch (e: Exception) {
            Log.e("FFmpegVideoProcessor", "動画結合中にエラー発生", e)
            finishProcessing()
            return@withContext false
        }
    }

    /**
     * FFmpegコマンドを実行する
     * @param command 実行するFFmpegコマンド
     * @return 処理が成功したかどうか
     */
    private suspend fun executeFFmpegCommand(command: String): Boolean = suspendCancellableCoroutine { continuation ->
        FFmpegKit.executeAsync(command, { session ->
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                Log.d("FFmpegVideoProcessor", "FFmpeg処理成功")
                continuation.resume(true)
            } else if (ReturnCode.isCancel(returnCode)) {
                Log.w("FFmpegVideoProcessor", "FFmpeg処理がキャンセルされました")
                continuation.resume(false)
            } else {
                // エラーの詳細情報を取得
                val output = session.output
                val logs = session.logs
                val failStackTrace = session.failStackTrace

                Log.e("FFmpeg", "エラー: ${returnCode}")
                Log.e("FFmpeg", "出力: ${output}")
                Log.e("FFmpeg", "ログ: ${logs}")
                Log.e("FFmpeg", "スタックトレース: ${failStackTrace}")
                continuation.resume(false)
            }
        }, { log ->
            Log.d("FFmpegVideoProcessor", log.message)
        }, { statistics ->
            // 進捗状況の更新（必要に応じて）
            val timeInMs = statistics.time
            Log.d("FFmpegVideoProcessor", "処理時間: $timeInMs ms")
        })
    }
}