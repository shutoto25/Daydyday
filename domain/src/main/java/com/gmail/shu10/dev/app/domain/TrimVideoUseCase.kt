package com.gmail.shu10.dev.app.domain

import android.content.Context
import android.net.Uri
import java.io.File
import javax.inject.Inject

/**
 * 動画トリミングUseCase
 */
class TrimVideoUseCase @Inject constructor(
    private val repository: IVideoEditorRepository
) {

    /**
     * 動画をトリミング
     * @param context Context
     * @param inputUri 入力動画URI
     * @param outputFile 出力ファイル
     * @param startTimeUs 開始時間（マイクロ秒）
     * @param durationUs 継続時間（マイクロ秒）
     * @return トリミング成功可否
     */
    suspend operator fun invoke(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        durationUs: Long = 1_000_000L
    ): Boolean {
        return repository.trimVideo(context, inputUri, outputFile, startTimeUs, durationUs)
    }
}
