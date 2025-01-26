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

        try {
            val mediaMuxer =
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024) // **バッファサイズを1MBに設定**

            var videoTrackIndex = -1
            var totalDurationUs = 0L // **累積時間を管理**
            val targetFrameRate = 30 // **QuickTime 互換のフレームレート**

            for (videoFile in videoFiles) {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, Uri.fromFile(videoFile), null)

                var videoTrack = -1
                var durationUs = 0L
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        videoTrack = i
                        durationUs = format.getLong(MediaFormat.KEY_DURATION)

                        // **QuickTime 互換の H.264 プロファイルを明示的に設定**
                        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3)
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, targetFrameRate)

                        // **QuickTime 互換の色空間を YUV420 に統一**
                        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)

                        if (videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer.addTrack(format)
                            mediaMuxer.start() // ✅ **最初のトラック追加時のみ start()**
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

                    // **QuickTime 互換のため presentationTimeUs を累積**
                    bufferInfo.presentationTimeUs = totalDurationUs + extractor.sampleTime
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME

                    mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)

                    extractor.advance()
                }

                // **累積時間を更新**
                totalDurationUs += durationUs
                extractor.release()
            }

            mediaMuxer.stop()
            mediaMuxer.release()

            Log.d("VideoMerger", "動画結合完了: ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("VideoMerger", "動画結合中にエラー発生", e)
        }
    }

    private fun get1secVideoFiles(context: Context): List<File> {
        val videoDir = File(context.filesDir, "videos/1sec")
        return videoDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }
}