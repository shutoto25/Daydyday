package com.gmail.shu10.dev.app.domain

import android.content.Context
import java.io.File
import javax.inject.Inject

/**
 * 画像から動画生成UseCase
 */
class CreateVideoFromImagesUseCase @Inject constructor(
    private val repository: IVideoEditorRepository
) {

    /**
     * 画像から動画を生成
     * @param context Context
     * @param imagePaths 画像パスリスト
     * @param outputFile 出力ファイル
     * @param frameRate フレームレート
     * @param durationPerImageMs 各画像の表示時間（ミリ秒）
     * @return 生成成功可否
     */
    suspend operator fun invoke(
        context: Context,
        imagePaths: List<String>,
        outputFile: File,
        frameRate: Int = 30,
        durationPerImageMs: Long = 2000L
    ): Boolean {
        return repository.createVideoFromImages(
            context,
            imagePaths,
            outputFile,
            frameRate,
            durationPerImageMs
        )
    }
}
