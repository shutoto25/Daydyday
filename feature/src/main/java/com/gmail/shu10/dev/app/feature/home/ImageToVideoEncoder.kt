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
import com.gmail.shu10.dev.app.feature.home.EGLHelper
import com.gmail.shu10.dev.app.feature.home.GLRenderer
import java.io.File

class ImageToVideoEncoder(
    private val outputFilePath: String,
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
        // MediaFormatの作成
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

        // EGL/GLの初期化（MediaCodecの入力Surfaceを使う）
        eglHelper = EGLHelper(inputSurface, width, height)
        glRenderer = GLRenderer(width, height)
    }

    /**
     * エンコードループ：1秒間分（frameRateフレーム）同じBitmapをレンダリングして動画を作成
     */
    fun encodeStillImage(bitmap: Bitmap) {
        prepareEncoder()

        // 1秒間のフレーム間隔（マイクロ秒）
        val frameIntervalUs = 1_000_000L / frameRate
        var presentationTimeUs = 0L

        // ループ：各フレームごとに同じ画像を描画してエンコード
        for (i in 0 until frameRate) {
            // OpenGLでBitmapをレンダリング
            glRenderer.render(bitmap)
            // フレームタイムスタンプの設定（単位：ナノ秒）
            eglHelper.swapBuffers(presentationTimeUs * 1000)  // us -> ns

            // エンコーダから出力をドレイン
            drainEncoder(endOfStream = false)

            presentationTimeUs += frameIntervalUs
        }

        // EOS送信してエンコード完了を待つ
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

                // EOSが検出された場合はループを抜ける
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 出力フォーマットが変更された場合
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
            } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break
                }
            }
        }
    }

    /**
     * エンコーダ、muxer、EGLのリソース解放
     */
    private fun releaseResources() {
        try {
            encoder.stop()
            encoder.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            muxer.stop()
            muxer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            eglHelper.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        inputSurface.release()
    }
}