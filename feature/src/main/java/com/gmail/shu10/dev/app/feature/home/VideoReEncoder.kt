package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import androidx.core.net.toUri
import java.io.File
class VideoReEncoder(
    private val context: Context,
    private val inputFile: File,
    private val outputFile: File,
    private val targetWidth: Int = 1920,
    private val targetHeight: Int = 1920,
    private val targetFrameRate: Int = 30,
    private val bitRate: Int = 2_000_000
) {
    private lateinit var extractor: MediaExtractor
    private lateinit var decoder: MediaCodec
    private lateinit var encoder: MediaCodec
    private lateinit var muxer: MediaMuxer
    private lateinit var encoderInputSurface: Surface

    // デコーダ出力用：SurfaceTexture とそれに紐付く Surface
    private lateinit var decoderSurfaceTexture: SurfaceTexture
    private lateinit var decoderOutputSurface: Surface
    private var decoderTextureId: Int = 0

    // EGL/GL用
    private lateinit var eglHelper: EGLHelper
    private lateinit var glRenderer: GLRenderer

    // Muxer 用トラック
    private var muxerTrackIndex: Int = -1
    private val bufferInfo = MediaCodec.BufferInfo()

    fun transcode() {
        // 1. Extractor を初期化して、動画トラックを選択
        extractor = MediaExtractor()
        extractor.setDataSource(context, inputFile.absolutePath.toUri(), null)
        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                videoTrackIndex = i
                break
            }
        }
        if (videoTrackIndex < 0) {
            throw RuntimeException("No video track found in ${inputFile.absolutePath}")
        }
        extractor.selectTrack(videoTrackIndex)
        val inputFormat = extractor.getTrackFormat(videoTrackIndex)

        // 2. Decoder の初期化
        val decoderMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw RuntimeException("No MIME type in input format")
        // まず、生成する OpenGL テクスチャ ID を取得
        decoderTextureId = generateExternalTexture()
        decoderSurfaceTexture = SurfaceTexture(decoderTextureId).apply {
            // 必要なら入力動画のサイズに合わせる。ここでは入力フォーマットの幅・高さを使います。
            setDefaultBufferSize(inputFormat.getInteger(MediaFormat.KEY_WIDTH), inputFormat.getInteger(MediaFormat.KEY_HEIGHT))
        }
        decoderOutputSurface = Surface(decoderSurfaceTexture)
        decoder = MediaCodec.createDecoderByType(decoderMime)
        decoder.configure(inputFormat, decoderOutputSurface, null, 0)
        decoder.start()

        // 3. Encoder の初期化（出力フォーマット固定：H.264 / 1920×1920 / 30fps）
        val outputFormat = MediaFormat.createVideoFormat("video/avc", targetWidth, targetHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, targetFrameRate)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        encoder = MediaCodec.createEncoderByType("video/avc")
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoderInputSurface = encoder.createInputSurface()
        encoder.start()

        // 4. Muxer の初期化
        if (outputFile.exists()) outputFile.delete()
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 5. EGLHelper と GLRenderer の初期化（encoderInputSurface を使用）
        eglHelper = EGLHelper(encoderInputSurface, targetWidth, targetHeight)
        glRenderer = GLRenderer(targetWidth, targetHeight)

        // 6. 再エンコードループ
        val timeoutUs = 10000L
        // フレーム間隔の計算：ここでは、最終フレームのタイムスタンプが正確に1秒（1,000,000μs）となるようにします。
        val totalFrames = targetFrameRate  // 例: 30
        val frameIntervalUs = if (totalFrames > 1) 1_000_000L / (totalFrames - 1) else 1_000_000L
        var frameIndex = 0

        var sawOutputEOS = false
        while (!sawOutputEOS) {
            // ① Decoder の出力を更新
            // 通常は、SurfaceTexture の updateTexImage() を呼び出して最新フレームを取得します
            decoderSurfaceTexture.updateTexImage()
            // SurfaceTexture の変換行列を取得
            val transformMatrix = FloatArray(16)
            decoderSurfaceTexture.getTransformMatrix(transformMatrix)

            // ② GLRenderer を用いて、decoderTextureId のテクスチャ（デコーダ出力）を encoderInputSurface に描画する
            // このメソッドは、decoderTextureId（GL_TEXTURE_EXTERNAL_OES）を使用して描画します。
            glRenderer.renderFrameFromDecoder(decoderTextureId, transformMatrix, targetWidth)

            // ③ 各フレームの presentationTimeUs を計算
            val presentationTimeUs = if (frameIndex == totalFrames - 1) {
                1_000_000L
            } else {
                frameIndex * frameIntervalUs
            }
            eglHelper.swapBuffers(presentationTimeUs * 1000) // μs -> ns
            frameIndex++
            if (frameIndex >= totalFrames) {
                // EOS: 終了したとみなす
                // ここでは単純にループ回数で終了させています。
                // 実際は decoder EOS を待つ処理が必要です。
                break
            }

            // ④ エンコーダから出力サンプルを取り出し、MediaMuxer に書き込む
            var encoderOutputBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            while (encoderOutputBufferId >= 0) {
                val encodedData = encoder.getOutputBuffer(encoderOutputBufferId)
                    ?: throw RuntimeException("Encoder output buffer $encoderOutputBufferId was null")
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    if (muxerTrackIndex < 0) {
                        val newFormat = encoder.outputFormat
                        muxerTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                    }
                    muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(encoderOutputBufferId, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                    break
                }
                encoderOutputBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            }
        }

        // 7. リソース解放
        muxer.stop()
        muxer.release()
        releaseResources()
    }

    private fun releaseResources() {
        try { decoder.stop(); decoder.release() } catch (e: Exception) { e.printStackTrace() }
        try { encoder.stop(); encoder.release() } catch (e: Exception) { e.printStackTrace() }
        try { eglHelper.release() } catch (e: Exception) { e.printStackTrace() }
        try { extractor.release() } catch (e: Exception) { e.printStackTrace() }
        decoderOutputSurface.release()
        decoderSurfaceTexture.release()
        encoderInputSurface.release()
    }

    // Utility: generate an external OES texture for SurfaceTexture
    private fun generateExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return textures[0]
    }
}