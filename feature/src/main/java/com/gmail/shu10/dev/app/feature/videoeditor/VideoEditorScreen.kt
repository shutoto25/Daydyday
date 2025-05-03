package com.gmail.shu10.dev.app.feature.videoeditor

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import com.gmail.shu10.dev.app.feature.videoeditor.section.VideoControlButtonsSection
import com.gmail.shu10.dev.app.feature.videoeditor.section.ThumbnailTimelineSection
import com.gmail.shu10.dev.app.feature.videoeditor.section.VideoPlayerSection
import com.google.common.collect.ImmutableList
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
    modifier: Modifier = Modifier,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val thumbnails by viewModel.thumbnails.collectAsState()
    val position by viewModel.position.collectAsState()

    // ExoPlayerの初期化
    LaunchedEffect(Unit) {
        viewModel.initializePlayer(context)
        viewModel.extractThumbnails(context, diary.videoPath?.toUri())
    }

    ViewEditContent(
        modifier = modifier,
        exoPlayer = viewModel.exoPlayer,
        videoUri = diary.videoPath?.toUri(),
        thumbnails = thumbnails,
        onTimeline = { startMs -> viewModel.updatePosition(startMs) },
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
    exoPlayer: ExoPlayer?,
    videoUri: Uri?,
    thumbnails: ImmutableList<Bitmap>,
    onTimeline: (Long) -> Unit,
    position: Long,
    onPreview: () -> Unit,
    onTrim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        VideoPlayerSection(
            startPositionMs = position,
            exoPlayer = exoPlayer,
            uri = videoUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        ThumbnailTimelineSection(
            thumbnails = thumbnails,
            onTrimRangeChanged = { startMs -> onTimeline(startMs) },
            modifier = Modifier.fillMaxWidth()
        )
        VideoControlButtonsSection(
            onPreview = { onPreview() },
            onTrim = { onTrim() },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}