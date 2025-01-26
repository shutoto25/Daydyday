package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.media.*
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor() : ViewModel() {

    fun mergeVideos(context: Context) {
        val targetDir = File(context.filesDir, "videos/merge")
        if (!targetDir.exists()) targetDir.mkdirs()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val outputFile = File(targetDir, "merge-$todayDate.mp4")

        val videoFiles = get1secVideoFiles(context)
            .sortedBy { it.nameWithoutExtension } // **日付順に並べる**

        if (videoFiles.isEmpty()) {
            Log.e("VideoMerger", "動画が見つかりませんでした")
            return
        }

        var mediaMuxer: MediaMuxer? = null
        var videoTrackIndex = -1
        var totalDurationUs = 0L // **累積時間を管理**
        var isMuxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(1024 * 1024) // **バッファサイズを1MBに設定**

        try {
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            for (videoFile in videoFiles) {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, Uri.fromFile(videoFile), null)

                var videoTrack = -1
                var videoDurationUs = 0L // **この動画の長さ**

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        videoTrack = i
                        videoDurationUs = format.getLong(MediaFormat.KEY_DURATION) // **正確な動画時間を取得**

                        Log.d("VideoMerger", "Extracted format from ${videoFile.name}: $format")

                        if (videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer.addTrack(format)
                            mediaMuxer.start()
                            isMuxerStarted = true
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

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        break
                    }

                    // **presentationTimeUs の適正化**
                    bufferInfo.presentationTimeUs = extractor.sampleTime + totalDurationUs

                    // **フラグを適切に変換**
                    bufferInfo.flags = when {
                        extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0 -> MediaCodec.BUFFER_FLAG_KEY_FRAME
                        extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0 -> MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                        else -> 0
                    }

                    mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)

                    extractor.advance()
                }

                // **次の動画の開始時間を適切に設定**
                totalDurationUs += videoDurationUs

                extractor.release()
            }

            if (isMuxerStarted) {
                mediaMuxer.stop()
            }

            Log.d("VideoMerger", "動画結合完了: ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("VideoMerger", "動画結合中にエラー発生", e)

        } finally {
            mediaMuxer?.release()
        }
    }

    private fun get1secVideoFiles(context: Context): List<File> {
        val videoDir = File(context.filesDir, "videos/1sec")
        return videoDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }
}