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
import kotlin.coroutines.resume

/**
 * FFmpegを利用した動画処理ユーティリティクラス
 */
class FFmpegVideoProcessor {

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

            // 開始時間（秒単位）
            val startSeconds = startMs / 1000f

            // 標準化された1秒動画切り出しコマンド（画像からの生成と同じ条件）
            val command = "-y -i $inputPath -ss $startSeconds " +
                    "-vf scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2 " +
                    "-t 1.0 " +              // 正確に1秒
                    "-r 30 " +               // 30fpsに統一
                    "-c:v mpeg4 " +          // mpeg4コーデック
                    "-q:v 5 " +              // 品質レベル
                    "-an " +                 // 音声なし
                    "-pix_fmt yuv420p " +    // 標準的なピクセルフォーマット
                    "${outputFile.absolutePath}"

            Log.d("FFmpegVideoProcessor", "トリミングコマンド: $command")

            // FFmpegコマンドを実行し、結果を待機
            val success = executeFFmpegCommand(command)

            if (success && outputFile.exists() && outputFile.length() > 0) {
                // 作成された動画の情報を確認
                val infoCommand = "-i ${outputFile.absolutePath}"
                executeFFmpegCommand(infoCommand) // 情報表示用なので結果は無視

                Log.d("FFmpegVideoProcessor", "動画トリミング成功: ${outputFile.absolutePath}, サイズ=${outputFile.length()} bytes")
                return@withContext true
            } else {
                Log.e("FFmpegVideoProcessor", "動画トリミング失敗")
                return@withContext false
            }
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

            // 標準化された1秒動画生成コマンド
            val command = "-y -loop 1 -i ${imageFile.absolutePath} " +
                    "-vf scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2 " +
                    "-t 1.0 " +              // 正確に1秒
                    "-r 30 " +               // 30fpsに統一
                    "-c:v mpeg4 " +          // mpeg4コーデック
                    "-q:v 5 " +              // 品質レベル
                    "-an " +                 // 音声なし
                    "-pix_fmt yuv420p " +    // 標準的なピクセルフォーマット
                    "${outputFile.absolutePath}"

            Log.d("FFmpegVideoProcessor", "画像→動画変換コマンド: $command")

            // FFmpegコマンドを実行し、結果を待機
            val success = executeFFmpegCommand(command)

            if (success && outputFile.exists() && outputFile.length() > 0) {
                // 作成された動画の情報を確認
                val infoCommand = "-i ${outputFile.absolutePath}"
                executeFFmpegCommand(infoCommand) // 情報表示用なので結果は無視

                Log.d("FFmpegVideoProcessor", "動画作成成功: ${outputFile.absolutePath}, サイズ=${outputFile.length()} bytes")
                return@withContext true
            } else {
                Log.e("FFmpegVideoProcessor", "動画作成失敗")
                return@withContext false
            }
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

            // 存在するファイルのみ抽出
            val validFiles = inputFiles.filter { it.exists() && it.length() > 0 }
            Log.d("FFmpegVideoProcessor", "有効な動画ファイル数: ${validFiles.size}")

            if (validFiles.isEmpty()) {
                Log.e("FFmpegVideoProcessor", "有効な動画ファイルがありません")
                return@withContext false
            }

            // リストファイルの準備
            val listFile = File(context.cacheDir, "input_list.txt")
            if (listFile.exists()) {
                listFile.delete()
            }

            // ファイルリストの作成
            listFile.printWriter().use { writer ->
                validFiles.forEach { file ->
                    writer.println("file '${file.absolutePath}'")
                    Log.d("FFmpegVideoProcessor", "リストに追加: ${file.absolutePath} (${file.length()} bytes)")
                }
            }

            // 動画を結合するコマンド - 各ファイルが既に標準化されているため単純なコピーで良い
            val command = "-y -f concat -safe 0 -i ${listFile.absolutePath} " +
                    "-c copy " +  // 既に標準化されているのでコピーだけで良い
                    "${outputFile.absolutePath}"

            Log.d("FFmpegVideoProcessor", "結合コマンド: $command")

            // FFmpegコマンドを実行し、結果を待機
            val result = executeFFmpegCommand(command)

            if (result && outputFile.exists() && outputFile.length() > 0) {
                // 結合後の動画の情報を確認
                val infoCommand = "-i ${outputFile.absolutePath}"
                executeFFmpegCommand(infoCommand) // 情報表示用なので結果は無視

                Log.d("FFmpegVideoProcessor", "動画結合成功: サイズ=${outputFile.length()} bytes")
                return@withContext true
            } else {
                Log.e("FFmpegVideoProcessor", "動画結合失敗または出力ファイルが無効")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("FFmpegVideoProcessor", "動画結合中にエラー発生", e)
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

    /**
     * 動画ファイルの詳細情報をログに記録する
     */
    private suspend fun logVideoInfo(context: Context, videoFile: File) {
        // 一時ファイルを作成してFFmpeg出力を保存
        val infoFile = File(context.cacheDir, "video_info.txt")
        if (infoFile.exists()) infoFile.delete()

        val infoCommand = "-i ${videoFile.absolutePath} -hide_banner"
        executeFFmpegCommand(infoCommand)  // 標準出力に情報を出力

        // フレーム数を取得するコマンド
        val frameCountCommand = "-i ${videoFile.absolutePath} -v error -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of csv=p=0"
        val frameCountFile = File(context.cacheDir, "frame_count.txt")
        if (frameCountFile.exists()) frameCountFile.delete()

        executeFFmpegCommand("$frameCountCommand > ${frameCountFile.absolutePath}")

        // フレーム数の読み取り
        val frameCount = if (frameCountFile.exists()) {
            try {
                frameCountFile.readText().trim().toIntOrNull() ?: "不明"
            } catch (e: Exception) {
                "読み取りエラー"
            }
        } else {
            "ファイルなし"
        }

        Log.d("VideoAnalysis", "ファイル: ${videoFile.name}, サイズ: ${videoFile.length()} bytes, フレーム数: $frameCount")
    }
}