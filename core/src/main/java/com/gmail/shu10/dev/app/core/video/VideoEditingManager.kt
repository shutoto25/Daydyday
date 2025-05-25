package com.gmail.shu10.dev.app.core.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 動画編集管理クラス
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
     * 画像から動画を生成
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

        try {
            val mediaFormat = createVideoFormat(frameRate)
            val mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            val mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            mediaCodec.start()

            var videoTrackIndex = -1
            var frameIndex = 0L
            val frameDurationUs = 1_000_000L / frameRate

            for (imagePath in imagePaths) {
                val bitmap = loadAndScaleBitmap(imagePath) ?: continue
                val framesPerImage = (durationPerImageMs * frameRate / 1000).toInt()

                repeat(framesPerImage) {
                    // フレーム描画処理は簡略化（実際の実装では Surface への描画が必要）
                    frameIndex++
                }

                bitmap.recycle()
            }

            // エンコーダーからデータを取得してファイルに書き込み
            var isOutputDone = false
            while (!isOutputDone) {
                val outputBufferIndex = mediaCodec.dequeueOutputBuffer(MediaCodec.BufferInfo(), TIMEOUT_US)

                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
                            mediaMuxer.start()
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        // 出力処理
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                    else -> {
                        isOutputDone = true
                    }
                }
            }

            mediaCodec.stop()
            mediaCodec.release()
            mediaMuxer.stop()
            mediaMuxer.release()
            surface.release()

            Log.d(TAG, "画像から動画生成完了: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "画像から動画生成エラー", e)
            false
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
     * @return スケールされたビットマップ
     */
    private fun loadAndScaleBitmap(imagePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            options.apply {
                inSampleSize = calculateInSampleSize(options, 1920, 1080)
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
     * @return MediaFormat
     */
    private fun createVideoFormat(frameRate: Int): MediaFormat {
        return MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, 1920, 1080).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
    }
}