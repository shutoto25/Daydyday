package com.gmail.shu10.dev.app.core.video


import android.content.Context
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
 * シンプルな動画作成クラス
 * 単一画像から短い動画を作成
 */
class SimpleVideoCreator {

    companion object {
        private const val TAG = "SimpleVideoCreator"
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val FRAME_RATE = 1 // 1fps（静止画風）
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_US = 10_000L
    }

    /**
     * 単一画像から1秒動画を作成（簡易版）
     * @param bitmap 入力画像
     * @param outputFile 出力ファイル
     * @param durationMs 動画の長さ（ミリ秒）
     * @return 成功可否
     */
    suspend fun createVideoFromBitmap(
        bitmap: Bitmap,
        outputFile: File,
        durationMs: Long = 1000L
    ): Boolean = withContext(Dispatchers.IO) {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            // エンコーダーの設定
            val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, bitmap.width, bitmap.height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000) // 1Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)

            encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            inputSurface = encoder.createInputSurface()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            encoder.start()

            // 画像を少数フレームで描画
            val canvas = inputSurface.lockCanvas(null)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // フレーム数は最小限（1fps × 1秒 = 1フレーム）
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            inputSurface.unlockCanvasAndPost(canvas)

            // エンコーディング終了
            encoder.signalEndOfInputStream()

            // 出力処理
            var muxerStarted = false
            var videoTrackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()

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

            Log.d(TAG, "シンプル動画作成完了: ${outputFile.absolutePath}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "シンプル動画作成エラー", e)
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
}
