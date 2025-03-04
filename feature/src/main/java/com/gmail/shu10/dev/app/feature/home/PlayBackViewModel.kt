package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.media.*
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor() : ViewModel() {

    // 結合後の動画 URI を保持する状態
    private val _mergedVideoUri = mutableStateOf<Uri?>(null)
    val mergedVideoUri: State<Uri?> get() = _mergedVideoUri

    /**
     * 1秒間の動画ファイルを連結して、ひとつの動画ファイルとして出力する。
     */
    fun mergeVideos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                concatenateVideos(context)
                val outputFile = File(context.filesDir, "videos/merged.mp4")
                if (outputFile.exists()) {
                    _mergedVideoUri.value = outputFile.toUri()
                    Log.d("VideoMerge", "動画連結完了: ${outputFile.absolutePath}")
                } else {
                    Log.e("VideoMerge", "出力ファイルが存在しません")
                }
            } catch (e: Exception) {
                Log.e("VideoMerge", "動画連結中にエラー発生", e)
            }
        }
    }

    /**
     * 指定ディレクトリ内の mp4 ファイルを日付順（ここではファイル名順）に連結し、
     * ひとつの動画ファイルとして出力する。
     *
     * @param context アプリケーションコンテキスト
     */
    private fun concatenateVideos(context: Context) {
        // 入力ディレクトリ
        val videoDir = File(context.filesDir, "videos/1sec")
        val videoFiles = videoDir.listFiles { file ->
            file.extension.equals("mp4", ignoreCase = true)
        }?.sortedBy { it.name }  // 日付名（ファイル名）順にソート
            ?: run {
                Log.e("VideoConcat", "動画ファイルが見つかりません")
                return
            }

        // 出力先ファイル
        val outputFile = File(context.filesDir, "videos/merged.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        try {
            // MediaMuxer の初期化（出力形式は MPEG_4）
            val muxer =
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 各動画ファイルのトラック情報は同一である前提
            var muxerTrackIndex = -1
            var muxerStarted = false
            // 各ファイルのタイムスタンプを連結するためのオフセット（マイクロ秒単位）
            var timeOffsetUs: Long = 0

            // バッファ（サンプルデータ格納用）
            val bufferSize = 1024 * 1024  // 1MB ほど（動画によっては大きくする必要がある場合も）
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // 1秒間に相当するフレーム数（例：30fpsなら30フレーム）
            val framesPerSecond = 30
            val defaultDurationUs = 1_000_000L // 1秒（マイクロ秒）

            // 各動画ファイルについて
            for (videoFile in videoFiles) {
                Log.d("VideoConcat", "処理開始: ${videoFile.absolutePath}")
                val extractor = MediaExtractor()
                extractor.setDataSource(videoFile.absolutePath)

                // 対象となる動画トラックを探す（動画のみを対象）
                var videoTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrackIndex = i
                        break
                    }
                }
                if (videoTrackIndex < 0) {
                    Log.e("VideoConcat", "動画トラックが見つかりません: ${videoFile.name}")
                    extractor.release()
                    continue
                }

                extractor.selectTrack(videoTrackIndex)
                val trackFormat = extractor.getTrackFormat(videoTrackIndex)

                // 最初のファイルの場合は muxer にトラックを追加して開始する
                if (!muxerStarted) {
                    muxerTrackIndex = muxer.addTrack(trackFormat)
                    muxer.start()
                    muxerStarted = true
                }

                // ファイル内の最後のサンプルタイムを記録する変数
                var lastSampleTimeUs = 0L

                // 動画ファイル内のサンプルを読み出し、muxer に書き込む
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        // ファイル終端
                        break
                    }
                    val sampleTime = extractor.sampleTime
                    lastSampleTimeUs = sampleTime  // 最後の有効なサンプルタイムを記録

                    // presentationTimeUs にオフセットを加算
                    bufferInfo.presentationTimeUs = sampleTime + timeOffsetUs

                    // extractor.sampleFlags から MediaCodec 用のフラグに変換する
                    val codecFlags = mapExtractorFlagsToCodecFlags(extractor.sampleFlags)
                    bufferInfo.flags = codecFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                // ファイル終了後のオフセット更新
                // もし lastSampleTimeUs が 0（＝サンプルタイムが取得できなかった場合）は、デフォルトで 1,000,000 マイクロ秒（1秒）とする
                val fileDurationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    trackFormat.getLong(MediaFormat.KEY_DURATION)
                } else {
                    if (lastSampleTimeUs > 0) lastSampleTimeUs else 1_000_000L
                }
                timeOffsetUs += fileDurationUs

                extractor.release()
                Log.d("VideoConcat", "処理完了: ${videoFile.name}")
            }

            // 終了処理
            muxer.stop()
            muxer.release()

            Log.d("VideoConcat", "連結完了: ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("VideoConcat", "動画連結中にエラー発生", e)
        }
    }
    // ．，：：：：：：：：：：：：：：：：：：：：：：」＿＿＿＿＿＿＿＿＿＿「lっっっっっっっっっっっっっっk」＠

    // extractor.sampleFlags から MediaCodec 用のフラグに変換するヘルパー関数
    private fun mapExtractorFlagsToCodecFlags(extractorFlags: Int): Int {
        var codecFlags = 0

        // SAMPLE_FLAG_SYNC が立っていれば、キーフレームとして扱う
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        // SAMPLE_FLAG_PARTIAL_FRAME が立っていれば、部分フレームフラグを設定する
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        // ※ SAMPLE_FLAG_ENCRYPTED については、用途に合わせた対応が必要です
        // ここでは特にマッピングしない例とします。

        return codecFlags
    }
}