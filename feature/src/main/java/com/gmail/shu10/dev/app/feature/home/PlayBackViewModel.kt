package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.media.*
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor() : ViewModel() {

    fun mergeVideos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            mergeVideosInternal(context)
        }
    }

    private fun mergeVideosInternal(context: Context): File? {
        val targetDir = File(context.filesDir, "videos/merge")
        if (!targetDir.exists()) targetDir.mkdirs()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val outputFile = File(targetDir, "merge-$todayDate.mp4")

        val videoFiles = get1secVideoFiles(context)
            .sortedBy { it.nameWithoutExtension } // **日付順に並べる**

        if (videoFiles.isEmpty()) {
            Log.e("VideoMerger", "動画が見つかりませんでした")
            return null
        }

        try {
            val mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024) // **バッファサイズを1MBに設定**

            var videoTrackIndex = -1
            var totalDurationUs = 0L
            var isStarted = false

            for (videoFile in videoFiles) {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, Uri.fromFile(videoFile), null)

                var videoTrack = -1
                var videoDurationUs = 0L

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/avc") == true) { // **H.264 のみ処理対象**
                        videoTrack = i
                        videoDurationUs = format.getLong(MediaFormat.KEY_DURATION)

                        Log.d("VideoMerger", "Processing: ${videoFile.name}, Duration: $videoDurationUs")

                        if (videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer.addTrack(format)
                        }
                        break
                    }
                }

                if (videoTrack == -1) {
                    Log.e("VideoMerger", "動画トラックが見つかりません: ${videoFile.name}")
                    extractor.release()
                    continue
                }

                extractor.selectTrack(videoTrack)

                if (!isStarted) {
                    mediaMuxer.start()
                    isStarted = true
                }

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        break
                    }

                    // **⚠ 修正: ノイズが入る問題を防ぐ**
                    val sampleTime = extractor.sampleTime
                    if (sampleTime < 0) break

                    bufferInfo.presentationTimeUs = totalDurationUs + sampleTime
                    bufferInfo.flags = convertSampleFlags(extractor.sampleFlags)

                    mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                totalDurationUs += videoDurationUs // **累積時間を更新**
                extractor.release()
            }

            if (isStarted) {
                mediaMuxer.stop()
            }
            mediaMuxer.release()
            Log.d("VideoMerger", "動画結合完了: ${outputFile.absolutePath}")

            return outputFile

        } catch (e: Exception) {
            Log.e("VideoMerger", "動画結合中にエラー発生", e)
            return null
        }
    }

    private fun get1secVideoFiles(context: Context): List<File> {
        val videoDir = File(context.filesDir, "videos/1sec")
        return videoDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }

    /**
     * `MediaExtractor.sampleFlags` を `MediaCodec.BufferInfo.flags` に変換
     */
    private fun convertSampleFlags(sampleFlags: Int): Int {
        var bufferFlags = 0
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            bufferFlags = bufferFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        return bufferFlags
    }
}