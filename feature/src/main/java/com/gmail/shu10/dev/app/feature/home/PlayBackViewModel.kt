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
import kotlin.math.min

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

    private fun concatenateVideos(context: Context) {
        // 入力ディレクトリ
        val videoDir = File(context.filesDir, "videos/1sec")
        val videoFiles = videoDir.listFiles { file ->
            file.extension.equals("mp4", ignoreCase = true)
        }?.sortedBy { it.name } ?: run {
            Log.e("VideoConcat", "動画ファイルが見つかりません")
            return
        }

        // 出力先ファイル
        val outputFile = File(context.filesDir, "videos/merged.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        try {
            // MediaMuxer の初期化
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var muxerTrackIndex = -1
            var muxerStarted = false
            var timeOffsetUs: Long = 0

            // 各ファイルの固定長さ )
            val fixedDurationUs = 1_000_000L

            // バッファ
            val bufferSize = 5 * 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // 各動画ファイルについて
            for (videoFileIndex in videoFiles.indices) {
                val videoFile = videoFiles[videoFileIndex]
                val isLastFile = videoFileIndex == videoFiles.size - 1

                Log.d("VideoConcat", "処理開始: ${videoFile.absolutePath}, ファイル ${videoFileIndex + 1}/${videoFiles.size}")

                val extractor = MediaExtractor()
                extractor.setDataSource(videoFile.absolutePath)

                // トラック検索
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

                // 初回のみMuxer開始
                if (!muxerStarted) {
                    muxerTrackIndex = muxer.addTrack(trackFormat)
                    muxer.start()
                    muxerStarted = true
                }

                // ファイル内の最初のサンプルタイムを記録（正規化用）
                var firstSampleTimeUs = -1L
                var lastWrittenTimeUs = -1L  // 前回書き込みタイムスタンプ

                // この動画ファイル用の個別オフセット
                val fileStartTimeUs = timeOffsetUs

                // 動画ファイル内のサンプルを処理
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size < 0) {
                        break  // ファイル終端
                    }

                    val sampleTime = extractor.sampleTime

                    // 最初のサンプルタイムを記録
                    if (firstSampleTimeUs < 0) {
                        firstSampleTimeUs = sampleTime
                    }

                    // 相対的なサンプルタイム（ファイル内で正規化）
                    val normalizedSampleTime = sampleTime - firstSampleTimeUs

                    // 現在のファイルの相対位置を制限する（最後のファイル以外）
                    if (!isLastFile && normalizedSampleTime > fixedDurationUs) {
                        Log.d("VideoConcat", "ファイル ${videoFile.name} の固定長さ ${fixedDurationUs}us を超えるフレームをスキップ")
                        extractor.advance()
                        continue
                    }

                    // presentationTimeとして設定するタイムスタンプ
                    bufferInfo.presentationTimeUs = fileStartTimeUs + normalizedSampleTime

                    // 前回のタイムスタンプより小さい場合は修正（念のため）
                    if (lastWrittenTimeUs >= 0 && bufferInfo.presentationTimeUs <= lastWrittenTimeUs) {
                        bufferInfo.presentationTimeUs = lastWrittenTimeUs + 1000 // 1ms加算
                    }

                    lastWrittenTimeUs = bufferInfo.presentationTimeUs

                    // フラグ設定
                    val codecFlags = mapExtractorFlagsToCodecFlags(extractor.sampleFlags)
                    bufferInfo.flags = codecFlags

                    // Muxerに書き込み
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                // ファイル間のオフセット更新
                // 最後のファイル以外は固定長を使用
                timeOffsetUs += if (isLastFile) {
                    // 最後のファイルは実際の長さを使用（最大でも固定長+10%まで）
                    val actualDuration = if (lastWrittenTimeUs > fileStartTimeUs)
                        (lastWrittenTimeUs - fileStartTimeUs) else fixedDurationUs

                    min(actualDuration, (fixedDurationUs * 1.1).toLong())
                } else {
                    fixedDurationUs
                }

                extractor.release()
                Log.d("VideoConcat", "処理完了: ${videoFile.name}, タイムオフセット: ${timeOffsetUs}us")
            }

            // 終了処理
            muxer.stop()
            muxer.release()

            Log.d("VideoConcat", "連結完了: ${outputFile.absolutePath}, 総時間: ${timeOffsetUs}us")
        } catch (e: Exception) {
            Log.e("VideoConcat", "動画連結中にエラー発生", e)
            e.printStackTrace()
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
        return codecFlags
    }
}