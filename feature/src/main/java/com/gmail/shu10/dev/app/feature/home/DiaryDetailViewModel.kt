package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class DiaryDetailViewModel @Inject constructor(
    private val saveDiaryUseCase: SaveDiaryUseCase,
) : ViewModel() {

    /**
     * 日記保存
     * @param diary 日記
     */
    fun saveDiary(diary: Diary) {
        viewModelScope.launch {
            saveDiaryUseCase(diary)
        }
    }

    /**
     * 写真保存
     * @param context Context
     * @param uri Uri
     * @param date 日付
     */
    fun savePhotoToAppDir(context: Context, uri: Uri, date: String): File? {

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val appDir = File(context.filesDir, "images")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, "$date.jpg")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        createVideoFromImage(BitmapFactory.decodeFile(file.path), targetFile(context, date))
        return file
    }

    /**
     * 動画保存
     */
    fun saveVideoToAppDir(context: Context, uri: Uri, date: String): File? {

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val appDir = File(context.filesDir, "videos")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, "$date.mp4")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun targetFile(context: Context, date: String): File {
        val appDir = File(context.filesDir, "videos/1sec")
        if (!appDir.exists()) appDir.mkdirs()
        val targetFile = File(appDir, "$date.mp4")
        return targetFile
    }

    private fun createVideoFromImage(
        image: Bitmap,
        outputFile: File,
    ) {
        // 動画の設定
        val width = image.width
        val height = image.height
        val frameRate = 30 // 30 FPS
        val durationSeconds = 1 // 1秒
        val totalFrames = frameRate * durationSeconds // 30フレーム

        val muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // ビットレート
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate) // フレームレート
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Iフレーム間隔
            setInteger(
                MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
            ) // プロファイルレベル
        }
        val videoTrackIndex = muxer.addTrack(format)
        muxer.start()

        // フレームごとに画像をエンコード
        val bufferInfo = MediaCodec.BufferInfo()
        val timestampIncrement = 1_000_000L / frameRate // 1フレームごとのタイムスタンプ (マイクロ秒)

        for (i in 0 until totalFrames) {
            bufferInfo.presentationTimeUs = i * timestampIncrement
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME

            // 画像をバイト配列に変換
            val byteBuffer = imageToByteBuffer(image)

            // Muxer にフレームを書き込む
            muxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
        }

        muxer.stop()
        muxer.release()

    }

    private fun imageToByteBuffer(image: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocate(image.byteCount)
        image.copyPixelsToBuffer(buffer)
        buffer.rewind() // 読み取り位置をリセット
        return buffer
    }
}