package com.gmail.shu10.dev.app.core.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * シンプルな動画作成クラス（改善版）
 * 単一画像から短い動画を作成
 */
class SimpleVideoCreator {

    companion object {
        private const val TAG = "SimpleVideoCreator"
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val FRAME_RATE = 1 // 1fps（静止画風）
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_US = 10_000L
        private const val DEFAULT_BIT_RATE = 1_000_000 // 1Mbps
    }

    /**
     * 単一画像から1秒動画を作成（改善版）
     * @param bitmap 入力画像
     * @param outputFile 出力ファイル
     * @param durationMs 動画の長さ（ミリ秒）
     * @param quality 動画品質設定
     * @return 成功可否
     */
    suspend fun createVideoFromBitmap(
        bitmap: Bitmap,
        outputFile: File,
        durationMs: Long = 1000L,
        quality: VideoQuality = VideoQuality.MEDIUM
    ): Boolean = withContext(Dispatchers.IO) {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            // 動画サイズをビットマップに合わせる（品質設定を考慮）
            val videoWidth = minOf(bitmap.width, quality.width)
            val videoHeight = minOf(bitmap.height, quality.height)

            // エンコーダーの設定
            val format = createVideoFormat(videoWidth, videoHeight, quality.bitRate)
            encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            inputSurface = encoder.createInputSurface()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            encoder.start()

            // 画像をSurfaceに描画（改善版）
            val success = drawBitmapToSurface(inputSurface, bitmap, videoWidth, videoHeight)
            if (!success) {
                Log.e(TAG, "Surface描画に失敗しました")
                return@withContext false
            }

            // エンコーディング終了シグナル
            encoder.signalEndOfInputStream()

            // 出力処理
            var muxerStarted = false
            var videoTrackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()
            var outputDone = false
            var frameCount = 0

            while (!outputDone) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // タイムアウト - 処理継続
                        Log.d(TAG, "Encoder output timeout, continuing...")
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            val format = encoder.outputFormat
                            Log.d(TAG, "Output format changed: $format")
                            videoTrackIndex = muxer.addTrack(format)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                            ?: throw RuntimeException("OutputBuffer is null")

                        if (bufferInfo.size > 0) {
                            if (!muxerStarted) {
                                throw RuntimeException("Muxer not started")
                            }

                            // フレームタイミングを設定
                            bufferInfo.presentationTimeUs = frameCount * (1_000_000L / FRAME_RATE)
                            
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            frameCount++
                            
                            Log.d(TAG, "Frame $frameCount written, pts=${bufferInfo.presentationTimeUs}")
                        }

                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                            Log.d(TAG, "End of stream reached")
                        }
                    }
                }
            }

            Log.d(TAG, "シンプル動画作成完了: ${outputFile.absolutePath} (${frameCount} frames)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "シンプル動画作成エラー", e)
            false
        } finally {
            // リソース解放（順序重要）
            try {
                encoder?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Encoder stop error", e)
            }

            try {
                encoder?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Encoder release error", e)
            }

            try {
                muxer?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer stop error", e)
            }

            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer release error", e)
            }

            try {
                inputSurface?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Surface release error", e)
            }
        }
    }

    /**
     * SurfaceにBitmapを安全に描画
     * @param surface 描画対象Surface
     * @param bitmap 描画するBitmap
     * @param targetWidth 目標幅
     * @param targetHeight 目標高さ
     * @return 描画成功可否
     */
    private fun drawBitmapToSurface(
        surface: Surface,
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Boolean {
        return try {
            // Canvas APIの代わりにMediaCodecのInputSurfaceを使用
            val canvas = surface.lockCanvas(null)
            
            // 背景をクリア
            canvas.drawColor(android.graphics.Color.BLACK)
            
            // ビットマップをスケールして中央に配置
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val scaleX = targetWidth.toFloat() / bitmap.width
            val scaleY = targetHeight.toFloat() / bitmap.height
            val scale = minOf(scaleX, scaleY)
            
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            val left = (targetWidth - scaledWidth) / 2
            val top = (targetHeight - scaledHeight) / 2
            
            // スケールされたビットマップを描画
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                scaledWidth.toInt(),
                scaledHeight.toInt(),
                true
            )
            
            canvas.drawBitmap(scaledBitmap, left, top, paint)
            
            // スケールされたビットマップを解放
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            
            surface.unlockCanvasAndPost(canvas)
            
            Log.d(TAG, "Surface描画完了 (${targetWidth}x${targetHeight})")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Surface描画エラー", e)
            false
        }
    }

    /**
     * 動画フォーマットを作成
     * @param width 幅
     * @param height 高さ
     * @param bitRate ビットレート
     * @return MediaFormat
     */
    private fun createVideoFormat(width: Int, height: Int, bitRate: Int): MediaFormat {
        return MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            
            // より安定したエンコーディングのための追加設定
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            setFloat(MediaFormat.KEY_FRAME_RATE, FRAME_RATE.toFloat())
        }
    }

    /**
     * 動画品質設定
     */
    enum class VideoQuality(val width: Int, val height: Int, val bitRate: Int) {
        LOW(720, 480, 500_000),
        MEDIUM(1280, 720, 1_000_000),
        HIGH(1920, 1080, 2_000_000)
    }
}
