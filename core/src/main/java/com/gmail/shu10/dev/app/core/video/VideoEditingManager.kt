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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 動画編集管理クラス（0KB問題修正版）
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
     * 動画トリミング
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startTimeUs 開始時間（マイクロ秒）
     * @param durationUs 継続時間（マイクロ秒）
     * @return トリミング成功可否
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long = 1_000_000L // デフォルト1秒
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputPath = getPathFromUri(context, inputUri) ?: return@withContext false

            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val trackIndexMap = mutableMapOf<Int, Int>()

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

            // 開始位置にシーク
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val endTimeUs = startTimeUs + durationUs

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
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            Log.d(TAG, "動画トリミング完了: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "動画トリミングエラー", e)
            false
        }
    }

    /**
     * 画像から動画を生成（0KB問題完全修正版）
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param frameRate フレームレート
     * @param durationPerImageMs 各画像の表示時間（ミリ秒）
     * @return 生成成功可否
     */
    suspend fun createVideoFromImages(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int = 30,
        durationPerImageMs: Long = 2000L
    ): Boolean = withContext(Dispatchers.IO) {
        if (imagePaths.isEmpty()) return@withContext false

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            Log.d(TAG, "画像から動画生成開始: ${imagePaths.size}枚の画像")

            // 最初の画像でサイズを決定
            val firstBitmap = BitmapFactory.decodeFile(imagePaths[0])
                ?: return@withContext false

            val videoWidth = 1280  // 固定解像度を使用
            val videoHeight = 720

            Log.d(TAG, "動画解像度: ${videoWidth}x${videoHeight}")

            // MediaFormatを作成
            val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, videoWidth, videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            // エンコーダー初期化
            encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()

            // Muxer初期化
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            encoder.start()
            Log.d(TAG, "エンコーダー開始")

            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // 総フレーム数を計算
            val framesPerImage = (durationPerImageMs * frameRate / 1000).toInt()
            val totalFrames = imagePaths.size * framesPerImage
            var frameIndex = 0
            val frameDurationUs = 1_000_000L / frameRate

            Log.d(TAG, "総フレーム数: $totalFrames, フレーム間隔: ${frameDurationUs}us")

            // 各画像を処理
            for (imageIndex in imagePaths.indices) {
                val bitmap = loadAndScaleBitmap(imagePaths[imageIndex], videoWidth, videoHeight)
                if (bitmap == null) {
                    Log.w(TAG, "画像読み込み失敗: ${imagePaths[imageIndex]}")
                    continue
                }

                Log.d(TAG, "画像 $imageIndex 処理中...")

                // 各画像を指定フレーム数分生成
                repeat(framesPerImage) {
                    // Surfaceに描画
                    val canvas = inputSurface.lockCanvas(null)
                    
                    // 背景を黒で塗りつぶし
                    canvas.drawColor(android.graphics.Color.BLACK)
                    
                    // ビットマップをcanvasの中央に描画
                    val left = (videoWidth - bitmap.width) / 2f
                    val top = (videoHeight - bitmap.height) / 2f
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(bitmap, left, top, paint)
                    
                    inputSurface.unlockCanvasAndPost(canvas)

                    // エンコーダー出力処理
                    drainEncoder(encoder, muxer, bufferInfo, frameIndex * frameDurationUs, false) { trackIndex ->
                        if (!muxerStarted) {
                            videoTrackIndex = trackIndex
                            muxerStarted = true
                        }
                    }

                    frameIndex++
                }

                bitmap.recycle()
            }

            firstBitmap.recycle()
            Log.d(TAG, "全画像処理完了: $frameIndex フレーム生成")

            // エンコーディング終了
            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, frameIndex * frameDurationUs, true) { trackIndex ->
                if (!muxerStarted) {
                    videoTrackIndex = trackIndex
                    muxerStarted = true
                }
            }

            Log.d(TAG, "画像から動画生成完了: ${outputFile.absolutePath}")
            Log.d(TAG, "出力ファイルサイズ: ${outputFile.length()} bytes")

            return@withContext outputFile.length() > 0

        } catch (e: Exception) {
            Log.e(TAG, "画像から動画生成エラー", e)
            false
        } finally {
            // リソース解放
            try {
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Encoder解放エラー", e)
            }

            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer解放エラー", e)
            }

            try {
                inputSurface?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Surface解放エラー", e)
            }
        }
    }

    /**
     * エンコーダーからデータを排出する
     * @param encoder MediaCodec
     * @param muxer MediaMuxer
     * @param bufferInfo BufferInfo
     * @param presentationTimeUs プレゼンテーション時間
     * @param endOfStream ストリーム終了フラグ
     * @param onTrackAdded トラック追加時のコールバック
     */
    private suspend fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTimeUs: Long,
        endOfStream: Boolean,
        onTrackAdded: (Int) -> Unit
    ) {
        if (endOfStream) {
            Log.d(TAG, "エンコーダー終了シグナル送信")
        }

        var attempts = 0
        val maxAttempts = 100

        while (attempts < maxAttempts) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (endOfStream) {
                        Log.d(TAG, "出力バッファ待機中...")
                        delay(10)
                        attempts++
                    } else {
                        break // 通常フレームの場合は待機しない
                    }
                }

                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = encoder.outputFormat
                    Log.d(TAG, "エンコーダー出力フォーマット変更: $newFormat")
                    val trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    onTrackAdded(trackIndex)
                    Log.d(TAG, "Muxer開始、ビデオトラック: $trackIndex")
                }

                outputBufferIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        ?: throw RuntimeException("出力バッファがnull")

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        Log.d(TAG, "コーデック設定データをスキップ")
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        bufferInfo.presentationTimeUs = presentationTimeUs
                        
                        try {
                            muxer.writeSampleData(0, outputBuffer, bufferInfo) // videoTrackIndex = 0 を想定
                            Log.d(TAG, "フレーム書き込み: ${bufferInfo.size} bytes, pts=${bufferInfo.presentationTimeUs}")
                        } catch (e: Exception) {
                            Log.e(TAG, "フレーム書き込みエラー", e)
                        }
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Log.d(TAG, "エンコーダー終了フラグ検出")
                        break
                    }
                }
            }
        }
    }

    /**
     * 複数動画を結合
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @return 結合成功可否
     */
    suspend fun mergeVideos(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (inputUris.isEmpty()) return@withContext false

        try {
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var currentTimeUs = 0L
            var isMuxerStarted = false

            for (inputUri in inputUris) {
                val inputPath = getPathFromUri(context, inputUri) ?: continue
                val extractor = MediaExtractor()
                extractor.setDataSource(inputPath)

                // 最初の動画でトラックを初期化
                if (!isMuxerStarted) {
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                        when {
                            mimeType.startsWith("video/") -> {
                                videoTrackIndex = muxer.addTrack(format)
                                extractor.selectTrack(i)
                            }
                            mimeType.startsWith("audio/") -> {
                                audioTrackIndex = muxer.addTrack(format)
                                extractor.selectTrack(i)
                            }
                        }
                    }
                    muxer.start()
                    isMuxerStarted = true
                } else {
                    // 後続の動画では既存のトラックを選択
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                        if (shouldIncludeTrack(mimeType)) {
                            extractor.selectTrack(i)
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

                    val trackIndex = when (extractor.sampleTrackIndex) {
                        0 -> videoTrackIndex
                        1 -> audioTrackIndex
                        else -> -1
                    }

                    if (trackIndex >= 0) {
                        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    }

                    extractor.advance()
                }

                currentTimeUs += getVideoDurationUs(inputPath)
                extractor.release()
            }

            muxer.stop()
            muxer.release()

            Log.d(TAG, "動画結合完了: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "動画結合エラー", e)
            false
        }
    }

    /**
     * URIからファイルパスを取得
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
     */
    private fun loadAndScaleBitmap(imagePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return null
            
            // アスペクト比を保持してスケール
            val scaleX = targetWidth.toFloat() / originalBitmap.width
            val scaleY = targetHeight.toFloat() / originalBitmap.height
            val scale = minOf(scaleX, scaleY)
            
            val scaledWidth = (originalBitmap.width * scale).toInt()
            val scaledHeight = (originalBitmap.height * scale).toInt()
            
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            
            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "ビットマップ読み込みエラー", e)
            null
        }
    }

    /**
     * トラックを含めるかどうかを判定
     */
    private fun shouldIncludeTrack(mimeType: String): Boolean {
        return mimeType.startsWith("video/") || mimeType.startsWith("audio/")
    }
}
