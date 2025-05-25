package com.gmail.shu10.dev.app.data

import android.content.Context
import android.net.Uri
import com.gmail.shu10.dev.app.core.video.VideoEditingManager
import com.gmail.shu10.dev.app.domain.IVideoEditorRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 動画編集リポジトリ実装
 */
@Singleton
class VideoEditorRepositoryImpl @Inject constructor() : IVideoEditorRepository {

    private val videoEditingManager = VideoEditingManager()

    override suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long
    ): Boolean {
        return videoEditingManager.trimVideo(context, inputUri, outputFile, startTimeUs, durationUs)
    }

    override suspend fun createVideoFromImages(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int,
        durationPerImageMs: Long
    ): Boolean {
        return videoEditingManager.createVideoFromImages(
            context,
            imagePaths,
            outputFile,
            frameRate,
            durationPerImageMs
        )
    }

    override suspend fun mergeVideos(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File
    ): Boolean {
        return videoEditingManager.mergeVideos(context, inputUris, outputFile)
    }
}