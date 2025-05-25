package com.gmail.shu10.dev.app.core.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.gmail.shu10.dev.app.domain.VideoEditingCallback
import com.gmail.shu10.dev.app.domain.VideoEditingError
import com.gmail.shu10.dev.app.domain.VideoEditingOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 動画編集管理クラス（改善版）
 * Android標準のMedia APIを使用した動画編集機能を提供
 */
class VideoEditingManager {

    companion object {
        private const val TAG = "VideoEditingManager"
        private const val TIMEOUT_US = 10_000L
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    }

    /**
     * 動画トリミング（改善版）
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startTimeUs 開始時間（マイクロ秒）
     * @param durationUs 継続時間（マイクロ秒）
     * @param callback 進捗コールバック
     * @return トリミング成功可否
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long = 1_000_000L,
        callback: VideoEditingCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            callback?.onProgress(0)
            
            val inputPath = getPathFromUri(context, inputUri)
            if (inputPath == null) {
                callback?.onError(VideoEditingError.INPUT_FILE_NOT_FOUND, "入力ファイルが見つかりません")
                return@withContext false
            }

            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val trackIndexMap = mutableMapOf<Int, Int>()

            callback?.onProgress(20)

            // トラックの設定
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (shouldIncludeTrack(mimeType)) {
                    val trackIndex = muxer.addTrack(format)
                    trackIndexMap[i] = trackIndex
                    extractor.selectTrack(i)
                }
            }

            muxer.start()
            callback?.onProgress(40)

            // 開始位置にシーク
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val endTimeUs = startTimeUs + durationUs
            
            var processedFrames = 0
            val totalFrames = estimateFrameCount(durationUs)

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endTimeUs) break

                val trackIndex = trackIndexMap[extractor.sampleTrackIndex]
                if (trackIndex != null) {
                    bufferInfo.apply {
                        offset = 0
                        size = sampleSize
                        this.presentationTimeUs = presentationTimeUs - startTimeUs
                        flags = extractor.sampleFlags
                    }

                    muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    
                    // プログレス更新
                    processedFrames++
                    val progress = 40 + (processedFrames * 50 / totalFrames).coerceAtMost(50)
                    callback?.onProgress(progress)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            callback?.onProgress(100)
            callback?.onComplete(true, outputFile.absolutePath)
            
            Log.d(TAG, "動画トリミング完了: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "動画トリミングエラー", e)
            callback?.onError(VideoEditingError.ENCODING_FAILED, "動画トリミング中にエラーが発生しました: ${e.message}")
            false
        }
    }

    /**
     * 画像から動画を生成（完全実装版）
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param frameRate フレームレート
     * @param durationPerImageMs 各画像の表示時間（ミリ秒）
     * @param options 動画編集オプション
     * @param callback 進捗コールバック
     * @return 生成成功可否
     */
    suspend fun createVideoFromImages(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int = 30,
        durationPerImageMs: Long = 2000L,
        options: VideoEditingOptions = VideoEditingOptions(),
        callback: VideoEditingCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (imagePaths.isEmpty()) {
            callback?.onError(VideoEditingError.INPUT_FILE_NOT_FOUND, "画像ファイルが指定されていません")
            return@withContext false
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            callback?.onProgress(0)

            // 最初の画像でサイズを決定
            val firstBitmap = loadAndScaleBitmap(imagePaths[0], options.quality.width, options.quality.height)
                ?: throw Exception("最初の画像を読み込めませんでした")

            val mediaFormat = createVideoFormat(frameRate, firstBitmap.width, firstBitmap.height, options.quality.bitRate)
            encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)

            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            encoder.start()
            callback?.onProgress(20)

            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // 各画像を処理
            val totalImages = imagePaths.size
            val framesPerImage = (durationPerImageMs * frameRate / 1000).toInt()
            var currentFrame = 0L
            val frameDurationUs = 1_000_000L / frameRate

            for (i in imagePaths.indices) {
                val bitmap = loadAndScaleBitmap(imagePaths[i], firstBitmap.width, firstBitmap.height)
                if (bitmap == null) {
                    Log.w(TAG, "画像読み込み失敗: ${imagePaths[i]}")
                    continue
                }

                // 各画像を指定フレーム数分描画
                repeat(framesPerImage) { frameInImage ->
                    // Surfaceに画像を描画
                    drawBitmapToSurface(inputSurface, bitmap)

                    // エンコーダーからの出力を処理
                    while (true) {
                        val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                        
                        when {
                            outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                            outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                if (!muxerStarted) {
                                    videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                                    muxer.start()
                                    muxerStarted = true
                                }
                            }
                            outputBufferIndex >= 0 -> {
                                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                                
                                if (bufferInfo.size > 0 && muxerStarted) {
                                    bufferInfo.presentationTimeUs = currentFrame * frameDurationUs
                                    outputBuffer?.let { buffer ->
                                        muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                                    }
                                }
                                
                                encoder.releaseOutputBuffer(outputBufferIndex, false)
                            }
                        }
                    }
                    
                    currentFrame++
                }

                bitmap.recycle()
                
                // プログレス更新
                val progress = 20 + ((i + 1) * 60 / totalImages)
                callback?.onProgress(progress)
            }

            firstBitmap.recycle()

            // エンコーディング終了
            encoder.signalEndOfInputStream()
            
            // 残りの出力を処理
            var outputDone = false
            while (!outputDone) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // タイムアウト
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        
                        if (bufferInfo.size > 0 && muxerStarted) {
                            outputBuffer?.let { buffer ->
                                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                            }
                        }
                        
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            callback?.onProgress(100)
            callback?.onComplete(true, outputFile.absolutePath)

            Log.d(TAG, "画像から動画生成完了: ${outputFile.absolutePath}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "画像から動画生成エラー", e)
            callback?.onError(VideoEditingError.ENCODING_FAILED, "画像から動画生成中にエラーが発生しました: ${e.message}")
            false
        } finally {
            // リソース解放
            encoder?.stop()
            encoder?.release()
            
            if (muxer != null) {
                try {
                    muxer.stop()
                    muxer.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Muxer解放エラー", e)
                }
            }
            
            inputSurface?.release()
        }
    }

    /**
     * 複数動画を結合（改善版）
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @param callback 進捗コールバック
     * @return 結合成功可否
     */
    suspend fun mergeVideos(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File,
        callback: VideoEditingCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (inputUris.isEmpty()) {
            callback?.onError(VideoEditingError.INPUT_FILE_NOT_FOUND, "結合する動画が指定されていません")
            return@withContext false
        }

        try {
            callback?.onProgress(0)
            
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var currentTimeUs = 0L
            var isMuxerStarted = false

            val totalVideos = inputUris.size

            for (i in inputUris.indices) {
                val inputUri = inputUris[i]
                val inputPath = getPathFromUri(context, inputUri)
                if (inputPath == null) {
                    Log.w(TAG, "動画パス取得失敗: $inputUri")
                    continue
                }

                val extractor = MediaExtractor()
                extractor.setDataSource(inputPath)

                // 最初の動画でトラックを初期化
                if (!isMuxerStarted) {
                    for (j in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(j)
                        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                        when {
                            mimeType.startsWith("video/") && videoTrackIndex == -1 -> {
                                videoTrackIndex = muxer.addTrack(format)
                                extractor.selectTrack(j)
                            }
                            mimeType.startsWith("audio/") && audioTrackIndex == -1 -> {
                                audioTrackIndex = muxer.addTrack(format)
                                extractor.selectTrack(j)
                            }
                        }
                    }
                    muxer.start()
                    isMuxerStarted = true
                } else {
                    // 後続の動画では既存のトラックを選択
                    for (j in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(j)
                        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                        if (shouldIncludeTrack(mimeType)) {
                            extractor.selectTrack(j)
                        }
                    }
                }

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.apply {
                        offset = 0
                        size = sampleSize
                        presentationTimeUs = extractor.sampleTime + currentTimeUs
                        flags = extractor.sampleFlags
                    }

                    val trackIndex = getTrackIndex(extractor.sampleTrackIndex, videoTrackIndex, audioTrackIndex)
                    if (trackIndex >= 0) {
                        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    }

                    extractor.advance()
                }

                currentTimeUs += getVideoDurationUs(inputPath)
                extractor.release()

                // プログレス更新
                val progress = ((i + 1) * 90 / totalVideos)
                callback?.onProgress(progress)
            }

            muxer.stop()
            muxer.release()

            callback?.onProgress(100)
            callback?.onComplete(true, outputFile.absolutePath)

            Log.d(TAG, "動画結合完了: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "動画結合エラー", e)
            callback?.onError(VideoEditingError.ENCODING_FAILED, "動画結合中にエラーが発生しました: ${e.message}")
            false
        }
    }

    /**
     * SurfaceにBitmapを描画（正しい実装）
     * @param surface 描画対象Surface
     * @param bitmap 描画するBitmap
     */
    private fun drawBitmapToSurface(surface: Surface, bitmap: Bitmap) {
        try {
            val canvas = surface.lockCanvas(null)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            surface.unlockCanvasAndPost(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "Surface描画エラー", e)
        }
    }

    /**
     * フレーム数を推定
     * @param durationUs 動画の長さ（マイクロ秒）
     * @return 推定フレーム数
     */
    private fun estimateFrameCount(durationUs: Long): Int {
        return ((durationUs / 1_000_000.0) * 30).toInt() // 30fps想定
    }

    /**
     * トラックインデックスを取得
     * @param sampleTrackIndex サンプルトラックインデックス
     * @param videoTrackIndex 動画トラックインデックス
     * @param audioTrackIndex 音声トラックインデックス
     * @return 対応するトラックインデックス
     */
    private fun getTrackIndex(sampleTrackIndex: Int, videoTrackIndex: Int, audioTrackIndex: Int): Int {
        return when (sampleTrackIndex) {
            0 -> videoTrackIndex
            1 -> audioTrackIndex
            else -> -1
        }
    }

    /**
     * URIからファイルパスを取得
     * @param context Context
     * @param uri URI
     * @return ファイルパス
     */
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val tempFile = File.createTempFile("temp_video", ".mp4", context.cacheDir)
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        tempFile.absolutePath
                    }
                }
                "file" -> uri.path
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "URIからパス取得エラー", e)
            null
        }
    }

    /**
     * 動画の長さを取得
     * @param videoPath 動画パス
     * @return 動画の長さ（マイクロ秒）
     */
    private fun getVideoDurationUs(videoPath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            (duration?.toLong() ?: 0) * 1000 // ミリ秒をマイクロ秒に変換
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    /**
     * ビットマップを読み込んでスケール
     * @param imagePath 画像パス
     * @param targetWidth 目標幅
     * @param targetHeight 目標高さ
     * @return スケールされたビットマップ
     */
    private fun loadAndScaleBitmap(imagePath: String, targetWidth: Int = 1920, targetHeight: Int = 1080): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            options.apply {
                inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                inJustDecodeBounds = false
            }

            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "ビットマップ読み込みエラー", e)
            null
        }
    }

    /**
     * サンプルサイズを計算
     * @param options BitmapFactory.Options
     * @param reqWidth 要求幅
     * @param reqHeight 要求高さ
     * @return サンプルサイズ
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * トラックを含めるかどうかを判定
     * @param mimeType MIMEタイプ
     * @return 含める場合はtrue
     */
    private fun shouldIncludeTrack(mimeType: String): Boolean {
        return mimeType.startsWith("video/") || mimeType.startsWith("audio/")
    }

    /**
     * 動画フォーマットを作成
     * @param frameRate フレームレート
     * @param width 幅
     * @param height 高さ
     * @param bitRate ビットレート
     * @return MediaFormat
     */
    private fun createVideoFormat(frameRate: Int, width: Int, height: Int, bitRate: Int): MediaFormat {
        return MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
    }
}
