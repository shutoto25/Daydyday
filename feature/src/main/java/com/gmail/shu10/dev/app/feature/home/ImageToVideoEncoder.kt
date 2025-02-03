package com.gmail.shu10.dev.app.feature.home

import android.graphics.Bitmap
import android.media.*
import android.view.Surface
import java.nio.ByteBuffer

/**
 * ImageToVideoEncoder は、固定出力解像度1920×1920で、
 * 入力Bitmapをアスペクト比を維持した状態でレターボックス配置（余白は黒）し、
 * 1秒間（例：30fpsなら30フレーム）の静止画動画を生成する。
 */
class ImageToVideoEncoder(
    private val outputFilePath: String,
    // 呼び出し側では入力画像サイズが渡されるが、出力は固定（1920×1920）とする
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 30,
    private val bitRate: Int = 2_000_000
) {

    private val mimeType = "video/avc"

    private lateinit var encoder: MediaCodec
    private lateinit var inputSurface: Surface
    private lateinit var muxer: MediaMuxer
    private var trackIndex: Int = -1
    private var muxerStarted = false

    // エンコーダ用のバッファ情報
    private val bufferInfo = MediaCodec.BufferInfo()

    // EGL/GLレンダリング関連
    private lateinit var eglHelper: EGLHelper
    private lateinit var glRenderer: GLRenderer

    /**
     * エンコーダ、MediaMuxer、EGL/GLの初期化を行う
     */
    private fun prepareEncoder() {
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)  // 1秒毎にキーフレーム
        }

        // エンコーダの生成と設定
        encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // 入力用Surfaceを取得
        inputSurface = encoder.createInputSurface()
        encoder.start()

        // MediaMuxerの初期化
        muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 固定サイズで初期化（1920×1920）
        eglHelper = EGLHelper(inputSurface, width, height)
        glRenderer = GLRenderer(width, height)
    }

    /**
     * Bitmap を元に1秒間（frameRate枚分）の静止画動画を生成する。
     * rotationDegrees により、画像の回転（必要なら画像中心を軸に回転）を適用する。
     */
    fun encodeStillImage(bitmap: Bitmap, rotationDegrees: Float) {
        prepareEncoder()

        val totalFrames = frameRate + 1
        // ループで全 totalFrames 枚のフレームを生成する
        for (i in 0 until totalFrames) {
            val presentationTimeUs = if (i == totalFrames - 1) {
                1_000_000L  // 最終フレームは正確に1秒
            } else {
                i * 1_000_000L / (totalFrames - 1)
            }
            glRenderer.render(bitmap, rotationDegrees)
            // swapBuffers に渡す単位はナノ秒
            eglHelper.swapBuffers(presentationTimeUs * 1000) // マイクロ秒→ナノ秒
            drainEncoder(endOfStream = false)
        }
        drainEncoder(endOfStream = true)
        releaseResources()
    }

    /**
     * エンコーダから出力バッファを取り出し、MediaMuxerへ書き込む
     */
    private fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            encoder.signalEndOfInputStream()
        }

        while (true) {
            val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputBufferId >= 0) {
                val encodedData = encoder.getOutputBuffer(outputBufferId)
                    ?: throw RuntimeException("Encoder output buffer $outputBufferId was null")
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    if (!muxerStarted) {
                        // 最初の出力フォーマット変更時にトラックを追加し、muxer開始
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(outputBufferId, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 出力フォーマットが変更された場合
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
            } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            }
        }
    }

    /**
     * エンコーダ、MediaMuxer、EGL の各リソースを解放する。
     */
    private fun releaseResources() {
        try { encoder.stop(); encoder.release() } catch (e: Exception) { e.printStackTrace() }
        try { muxer.stop(); muxer.release() } catch (e: Exception) { e.printStackTrace() }
        try { eglHelper.release() } catch (e: Exception) { e.printStackTrace() }
        inputSurface.release()
    }
}