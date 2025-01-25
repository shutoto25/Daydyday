package com.gmail.shu10.dev.app.feature.home

import TextureRenderer
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.io.File
import java.lang.RuntimeException

class ImageToVideoEncoder(
    private val outputFile: File,
    private val width: Int = 1920,
    private val height: Int = 1080,
    private val frameRate: Int = 30
) {

    private lateinit var mediaCodec: MediaCodec
    private lateinit var inputSurface: Surface
    private lateinit var eglCore: EGLCore
    private lateinit var textureRenderer: TextureRenderer
    private lateinit var mediaMuxer: MediaMuxer
    private var trackIndex = -1
    private var isMuxerStarted = false

    fun encodeBitmapToMp4(bitmap: Bitmap, durationSeconds: Int = 1) {
        val frameCount = frameRate * durationSeconds

        // 1️⃣ `MediaMuxer` の作成
        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 2️⃣ `MediaCodec` の設定
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4000000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        inputSurface = mediaCodec.createInputSurface()
        mediaCodec.start()

        // 3️⃣ OpenGL ES のセットアップ
        eglCore = EGLCore(inputSurface)
        textureRenderer = TextureRenderer(width, height)

        // 4️⃣ フレームを描画
        for (i in 0 until frameCount) {
            textureRenderer.drawFrame(bitmap)
            eglCore.swapBuffers()
            drainEncoder(false)
        }

        // 5️⃣ 終了処理
        drainEncoder(true)
        mediaCodec.stop()
        mediaCodec.release()
        eglCore.release()

        // 6️⃣ `MediaMuxer` を停止
        if (isMuxerStarted) {
            mediaMuxer.stop()
            mediaMuxer.release()
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            mediaCodec.signalEndOfInputStream()
        }

        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) throw RuntimeException("MediaMuxer already started")
                val newFormat = mediaCodec.outputFormat
                trackIndex = mediaMuxer.addTrack(newFormat)
                mediaMuxer.start()
                isMuxerStarted = true
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null) {
                    if (bufferInfo.size > 0 && isMuxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }
    }
}