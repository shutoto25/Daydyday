package com.gmail.shu10.dev.app.domain

import android.content.Context
import android.net.Uri
import java.io.File
import javax.inject.Inject

/**
 * 動画結合UseCase
 */
class MergeVideosUseCase @Inject constructor(
    private val repository: IVideoEditorRepository
) {

    /**
     * 複数動画を結合
     * @param context Context
     * @param inputUris 入力動画URIリスト
     * @param outputFile 出力ファイル
     * @return 結合成功可否
     */
    suspend operator fun invoke(
        context: Context,
        inputUris: List<Uri>,
        outputFile: File
    ): Boolean {
        return repository.mergeVideos(context, inputUris, outputFile)
    }
}