package com.gmail.shu10.dev.app.domain

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 動画編集リポジトリインターフェース
 */
interface IVideoEditorRepository {

    /**
     * 動画をトリミング
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startTimeUs 開始時間（マイクロ秒）
     * @param durationUs 継続時間（マイクロ秒）
     * @return トリミング成功可否
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long = 1_000_000L
    ): Boolean

    /**
     * 画像から動画を生成
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param frameRate フレームレート
     * @param durationPerImageMs 各画像の表示時間（ミリ秒）
     * @return 生成成功可否
     */
    suspend fun createVideoFromImages(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int = 30,
        durationPerImageMs: Long = 2000L
    ): Boolean

    /**
     * 複数動画を結合
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @return 結合成功可否
     */
    suspend fun mergeVideos(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File
    ): Boolean
}