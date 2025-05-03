package com.gmail.shu10.dev.app.feature.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import com.gmail.shu10.dev.app.feature.videoeditor.section.VideoControlButtonsSection
import com.gmail.shu10.dev.app.feature.videoeditor.section.ThumbnailTimelineSection
import com.gmail.shu10.dev.app.feature.videoeditor.section.VideoPlayerSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 動画編集画面
 * @param navHostController ナビゲーションコントローラー
 * @param diary 日記データ
 * @param viewModel VideoEditorViewModel
 */
@Composable
fun VideoEditorScreen(
    navHostController: NavHostController,
    diary: Diary,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // 動画再生プレイヤー
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    // サムネイルリスト
    val thumbnails = remember { viewModel.extractThumbnails(context, diary.videoPath?.toUri()) }
    // 動画再生位置
    var position by remember { mutableLongStateOf(0L) }

    ViewEditContent(
        context = context,
        exoPlayer = exoPlayer,
        videoUri = diary.videoPath?.toUri(),
        thumbnails = thumbnails,
        onTimeline = { startMs -> position = startMs },
        position = position,
        onPreview = { /* プレビュー */ },
        onTrim = {
            diary.videoPath?.toUri()?.let {
                viewModel.startReEncoding(
                    context,
                    it,
                    viewModel.targetFile(context, diary.date),
                    position,
                    onSuccess = {
                        // トリミング成功
                        Log.d("TEST", "ViewEditScreenContent() called trim success")
                        val saveData =
                            diary.copy(
                                trimmedVideoPath = viewModel.targetFile(context, diary.date)
                                    .toContentUri(context).toString()
                            )
                        val json = Json.encodeToString(saveData)
                        navHostController.previousBackStackEntry?.savedStateHandle?.set(
                            "updateDiaryWithTrimmedVideo",
                            json
                        )
                        navHostController.popBackStack()
                    },
                    onError = {
                        // トリミング失敗
                        Log.d("TEST", "ViewEditScreenContent() called trim error")
                    }
                )
            }
        }
    )
}

/**
 * 動画編集画面コンテンツ
 */
@Composable
private fun ViewEditContent(
    context: Context,
    exoPlayer: ExoPlayer,
    videoUri: Uri?,
    thumbnails: List<Bitmap>,
    onTimeline: (Long) -> Unit,
    position: Long,
    onPreview: () -> Unit,
    onTrim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        VideoPlayerSection(
            context = context,
            startPositionMs = position,
            exoPlayer = exoPlayer,
            uri = videoUri
        )
        ThumbnailTimelineSection(
            thumbnails = thumbnails,
            onTrimRangeChanged = { startMs -> onTimeline(startMs) }
        )
        VideoControlButtonsSection(
            onPreview = { onPreview() },
            onTrim = { onTrim() }
        )
    }
}