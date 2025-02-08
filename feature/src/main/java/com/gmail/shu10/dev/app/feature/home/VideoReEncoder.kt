package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

class VideoReEncoder(
    private val context: Context,
    private val inputUri: Uri,
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

    // エンコード済みサンプルを保持するリスト
    private val sampleList = mutableListOf<EncodedSample>()

    /**
     * startMs はミリ秒単位の開始時刻。
     * その位置から1秒間の映像を再エンコードして出力する。
     */
    fun transcode(startMs: Long) {
        // 1. Extractor の初期化とシーク
        extractor = MediaExtractor()
        extractor.setDataSource(context, inputUri, null)
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
            throw RuntimeException("No video track found in $inputUri")
        }
        extractor.selectTrack(videoTrackIndex)
        // MediaExtractor のタイムスタンプはマイクロ秒単位
        val startUs = startMs * 1000L
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val inputFormat = extractor.getTrackFormat(videoTrackIndex)

        // 2. Decoder の初期化
        val decoderMime = inputFormat.getString(MediaFormat.KEY_MIME)
            ?: throw RuntimeException("No MIME type in input format")
        decoderTextureId = generateExternalTexture()
        decoderSurfaceTexture = SurfaceTexture(decoderTextureId).apply {
            setDefaultBufferSize(
                inputFormat.getInteger(MediaFormat.KEY_WIDTH),
                inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            )
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
        // isMp4 = true として、GLRenderer は外部テクスチャ用のシェーダーを使用
        glRenderer = GLRenderer(targetWidth, targetHeight, true)

        // 6. 再エンコードループ
// 出力フレーム数を targetFrameRate 枚として、0〜1,000,000μs を均等に割る
        val totalFrames = targetFrameRate  // 例: 30
        val frameIntervalUs = if (totalFrames > 1) 1_000_000L / (totalFrames - 1) else 1_000_000L
        var frameIndex = 0
        val timeoutUs = 10000L

        while (frameIndex < totalFrames) {
            // ① 入力側：デコーダへの入力データを供給する
            val inputBufferId = decoder.dequeueInputBuffer(timeoutUs)
            if (inputBufferId >= 0) {
                val inputBuffer = decoder.getInputBuffer(inputBufferId)
                val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                if (sampleSize < 0) {
                    // 終端に達したのでEOSフラグを送信
                    decoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    val sampleTime = extractor.sampleTime
                    decoder.queueInputBuffer(inputBufferId, 0, sampleSize, sampleTime, 0)
                    extractor.advance()
                }
            }

            // ② 出力側：デコーダからフレームを取得
            val outputBufferId = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputBufferId >= 0) {
                // 出力バッファをレンダリング（true を指定）
                decoder.releaseOutputBuffer(outputBufferId, true)
                // ここで、デコーダが出力したフレームが SurfaceTexture に反映されるのを待つ
                // updateTexImage() を呼び出して最新フレームを取得
                decoderSurfaceTexture.updateTexImage()
                val transformMatrix = FloatArray(16)
                if (decoderSurfaceTexture.timestamp == 0L) {
                    Matrix.setIdentityM(transformMatrix, 0)
                    Log.d("TEST", "Using identity matrix for transformMatrix")
                } else {
                    decoderSurfaceTexture.getTransformMatrix(transformMatrix)
                }
                Log.d("TEST", "Transform Matrix: ${transformMatrix.joinToString()}")

                // ③ GLRenderer でデコーダ出力テクスチャを encoderInputSurface に描画する
                glRenderer.renderFrameFromDecoder(decoderTextureId, transformMatrix, targetWidth)

                // ④ 希望する出力フレームの presentationTimeUs を計算し、swapBuffers() で出力
                val presentationTimeUs = if (frameIndex == totalFrames - 1) {
                    1_000_000L
                } else {
                    frameIndex * frameIntervalUs
                }
                eglHelper.swapBuffers(presentationTimeUs * 1000) // μs -> ns

                // エンコーダ出力をドレインしてサンプルリストに蓄積
                drainEncoderCollectSamples(presentationTimeUs)
                frameIndex++
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 出力フォーマット変更時の処理（必要に応じて）
            } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 特に何もせずループ継続
            }
        }

        // EOS をエンコーダに送信
        encoder.signalEndOfInputStream()

        // EOS 後のエンコーダ出力をすべてドレイン
        drainEncoderCollectSamples(1_000_000L)

        // 7. タイムスタンプ再スケーリング（全サンプルの最大タイムスタンプに合わせてスケーリング）
        val maxTs = sampleList.maxOfOrNull { it.presentationTimeUs } ?: 1_000_000L
        val scaleFactor = 1_000_000.0 / maxTs
        sampleList.forEach { sample ->
            sample.presentationTimeUs = (sample.presentationTimeUs * scaleFactor).toLong()
        }

        // 8. Muxer に書き出し
        muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
        muxer.start()
        sampleList.forEach { sample ->
            val info = MediaCodec.BufferInfo().apply {
                offset = 0
                size = sample.data.limit() - sample.data.position()
                presentationTimeUs = sample.presentationTimeUs
                flags = sample.flags
            }
            muxer.writeSampleData(muxerTrackIndex, sample.data, info)
        }
        muxer.stop()
        muxer.release()

        // 9. リソース解放
        releaseResources()
    }

    private fun drainEncoderCollectSamples(overrideTimestamp: Long) {
        val timeoutUs = 10000L
        while (true) {
            val encoderOutputBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encoderOutputBufferId >= 0) {
                val encodedData = encoder.getOutputBuffer(encoderOutputBufferId)
                    ?: throw RuntimeException("Encoder output buffer $encoderOutputBufferId was null")
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    // ここでは overrideTimestamp で上書きしてサンプルを作成
                    val sample = EncodedSample(
                        data = ByteBuffer.allocate(bufferInfo.size).apply {
                            put(encodedData)
                            flip()
                        },
                        presentationTimeUs = overrideTimestamp,
                        flags = bufferInfo.flags
                    )
                    sampleList.add(sample)
                }
                encoder.releaseOutputBuffer(encoderOutputBufferId, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            } else if (encoderOutputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 必要に応じて処理
            } else if (encoderOutputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }
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

data class EncodedSample(
    val data: ByteBuffer,          // エンコード済みのバイトデータ
    var presentationTimeUs: Long,  // presentationTimeUs（マイクロ秒単位）
    val flags: Int                 // バッファフラグ（例：キーフレームなど）
)