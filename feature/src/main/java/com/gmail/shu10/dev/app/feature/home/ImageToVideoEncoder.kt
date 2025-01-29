import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Mp4Muxer
import androidx.media3.transformer.*
import com.gmail.shu10.dev.app.feature.home.EGLCore
import java.io.File

class ImageToVideoEncoder(
    private val outputFile: File,
    private val bitmap: Bitmap,
) {
    companion object {
        private const val DURATION_SEC = 1 // 1秒間
        private const val FRAME_RATE = 30  // 30fps
    }

    private val frameCount = DURATION_SEC * FRAME_RATE
    private val width = bitmap.width
    private val height = bitmap.height

    private lateinit var mediaCodec: MediaCodec
    private lateinit var inputSurface: Surface
    private lateinit var mediaMuxer: MediaMuxer
    private var trackIndex = -1
    private var isMuxerStarted = false

    @OptIn(UnstableApi::class)
//    fun media3(context: Context) {
//
//
//        val transformerListener: Transformer.Listener =
//            object : Transformer.Listener {
//                override fun onCompleted(composition: Composition, result: ExportResult) {
//                }
//
//                override fun onError(
//                    composition: Composition, result: ExportResult,
//                    exception: ExportException,
//                ) {
//                }
//            }
//        val transformer = Transformer.Builder(context)
//            .setVideoMimeType(MimeTypes.VIDEO_H264)
//            .addListener(transformerListener)
//            .setMuxerFactory(Mp4Muxer.Builder())
//            .build()
//
//    }

    fun encodeBitmapToMp4() {

        // **1️⃣ `MediaMuxer` の作成**
        mediaMuxer = MediaMuxer(
            outputFile.absolutePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        ) // ファイルフォーマット

        // **2️⃣ `MediaFormat` の設定（動画の長さが0秒にならないようにする）**
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4000000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, DURATION_SEC)  // **フレームレートを設定**
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)    // **Iフレームを適切に設定**

        // **3️⃣ `MediaCodec` の設定**
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        inputSurface = mediaCodec.createInputSurface()
        mediaCodec.start()

        // **4️⃣ OpenGL ES を使用して描画**
        val eglCore = EGLCore(inputSurface)
        val textureRenderer = TextureRenderer(width, height, bitmap)

        for (i in 0 until frameCount) {
            textureRenderer.drawFrame()
            eglCore.swapBuffers()
            drainEncoder(false)
        }

        // **5️⃣ すべてのフレームをエンコードした後に `signalEndOfInputStream()` を呼ぶ**
        drainEncoder(true)
        mediaCodec.stop()
        mediaCodec.release()
        eglCore.release()

        // **6️⃣ `MediaMuxer` を停止（これがないと再生時間が 0 秒になることがある）**
        if (isMuxerStarted) {
            mediaMuxer.stop()
            mediaMuxer.release()
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            Log.d("TEST", "エンコード完了: signalEndOfInputStream() を呼ぶ")
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