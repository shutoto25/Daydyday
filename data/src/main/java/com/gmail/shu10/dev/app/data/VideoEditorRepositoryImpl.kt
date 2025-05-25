package com.gmail.shu10.dev.app.data

import android.content.Context
import android.net.Uri
import com.gmail.shu10.dev.app.core.video.VideoEditingManager
import com.gmail.shu10.dev.app.domain.IVideoEditorRepository
import com.gmail.shu10.dev.app.domain.VideoEditingCallback
import com.gmail.shu10.dev.app.domain.VideoEditingOptions
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 動画編集リポジトリ実装（改善版）
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

    /**
     * 動画をトリミング（コールバック付き）
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startTimeUs 開始時間（マイクロ秒）
     * @param durationUs 継続時間（マイクロ秒）
     * @param callback 進捗コールバック
     * @return トリミング成功可否
     */
    suspend fun trimVideoWithCallback(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long = 1_000_000L,
        callback: VideoEditingCallback? = null
    ): Boolean {
        return videoEditingManager.trimVideo(
            context, inputUri, outputFile, startTimeUs, durationUs, callback
        )
    }

    /**
     * 画像から動画を生成（コールバック付き）
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param frameRate フレームレート
     * @param durationPerImageMs 各画像の表示時間（ミリ秒）
     * @param options 動画編集オプション
     * @param callback 進捗コールバック
     * @return 生成成功可否
     */
    suspend fun createVideoFromImagesWithCallback(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int = 30,
        durationPerImageMs: Long = 2000L,
        options: VideoEditingOptions = VideoEditingOptions(),
        callback: VideoEditingCallback? = null
    ): Boolean {
        return videoEditingManager.createVideoFromImages(
            context,
            imagePaths,
            outputFile,
            frameRate,
            durationPerImageMs,
            options,
            callback
        )
    }

    /**
     * 複数動画を結合（コールバック付き）
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @param callback 進捗コールバック
     * @return 結合成功可否
     */
    suspend fun mergeVideosWithCallback(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File,
        callback: VideoEditingCallback? = null
    ): Boolean {
        return videoEditingManager.mergeVideos(context, inputUris, outputFile, callback)
    }
}
