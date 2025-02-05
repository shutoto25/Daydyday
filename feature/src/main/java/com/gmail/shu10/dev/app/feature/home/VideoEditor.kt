package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 動画編集画面
 * @param navHostController ナビゲーションコントローラー
 * @param diary 日記データ
 */
@Composable
fun VideoEditorScreen(
    navHostController: NavHostController,
    diary: Diary,
//    viewModel: DiaryDetailViewModel = hiltViewModel()
) {
    val viewModel: VideoEditorViewModel = hiltViewModel()

    val context = LocalContext.current
    // 動画再生プレイヤー
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    // サムネイルリスト
    val thumbnails = remember { viewModel.extractThumbnails(context, diary.videoPath?.toUri()) }
    // 動画再生位置
    var position by remember { mutableLongStateOf(0L) }
    ViewEditScreenContent(
        context = context,
        viewModel = viewModel,
        exoPlayer = exoPlayer,
        videoUri = diary.videoPath?.toUri(),
        thumbnails = thumbnails,
        onTimeline = { startMs -> position = startMs },
        position = position,
        onPreview = { /* プレビュー */ },
        onTrim = {
            diary.videoPath?.toUri()?.let {
//                viewModel.trimVideo(
//                    context = context,
//                    inputUri = it,
//                    outputFile = viewModel.targetFile(context, diary.date),
//                    startMs = position,
//                    onSuccess = {
//                        // トリミング成功
//                        Log.d("TEST", "ViewEditScreenContent() called trim success")
//                        val saveData =
//                            diary.copy(trimmedVideoPath = viewModel.targetFile(context, diary.date).toContentUri(context).toString())
//                        val json = Json.encodeToString(saveData)
//                        navHostController.previousBackStackEntry?.savedStateHandle?.set("updateDiaryWithTrimmedVideo", json)
//                        navHostController.popBackStack()
//                    },
//                    onError = {
//                        // トリミング失敗
//                        Log.d("TEST", "ViewEditScreenContent() called trim error")
//                    }
//                )
                viewModel.startReEncoding(
                    context,
                    it,
                    viewModel.targetFile(context, diary.date),
                    onSuccess = {
                        // トリミング成功
                        Log.d("TEST", "ViewEditScreenContent() called trim success")
                        val saveData =
                            diary.copy(trimmedVideoPath = viewModel.targetFile(context, diary.date).toContentUri(context).toString())
                        val json = Json.encodeToString(saveData)
                        navHostController.previousBackStackEntry?.savedStateHandle?.set("updateDiaryWithTrimmedVideo", json)
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
fun ViewEditScreenContent(
    context: Context,
    viewModel: VideoEditorViewModel,
    exoPlayer: ExoPlayer,
    videoUri: Uri?,
    thumbnails: List<Bitmap>,
    onTimeline: (Long) -> Unit,
    position: Long,
    onPreview: () -> Unit,
    onTrim: () -> Unit,
) {
    Column {
        VideoPlayer(
            context = context,
            startPositionMs = position,
            exoPlayer = exoPlayer,
            uri = videoUri
        )
        ThumbnailTimeline(thumbnails = thumbnails) { startMs -> onTimeline(startMs) }
        VideoControlButtons(
            onPreview = { onPreview() },
            onTrim = { onTrim() }
        )
    }
}

/**
 * 動画再生プレイヤー
 * @param context context
 * @param exoPlayer 動画再生プレイヤー
 * @param uri 動画URI
 */
@Composable
fun VideoPlayer(
    context: Context,
    startPositionMs: Long,
    exoPlayer: ExoPlayer,
    uri: Uri?,
) {
    uri?.let {
        DisposableEffect(Unit) {
            exoPlayer.apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
            // 画面破棄のタイミングでPlayerを解放
            onDispose { exoPlayer.release() }
        }
        LaunchedEffect(startPositionMs) {
            exoPlayer.seekTo(startPositionMs)
        }
        // PlayerViewとexpPlayerをAndroidView経由で統合
        AndroidView(
            factory = { PlayerView(context).apply { this.player = exoPlayer } },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
    }
}

/**
 * サムネイルタイムライン
 * @param thumbnails サムネイルリスト
 */
@Composable
fun ThumbnailTimeline(
    thumbnails: List<Bitmap>,
    onTrimRangeChanged: (startMs: Long) -> Unit,
) {
    var offset by remember { mutableStateOf(0.dp) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val timelineHeight = 100.dp
    val thumbnailWidth = calculateTrimWidth(thumbnails[0], timelineHeight)
    val trimWidth = thumbnailWidth * 2 // 1秒分
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                offset = with(density) { (size.width).toDp() / 2 - thumbnailWidth }
            }
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = offset, end = offset)
        ) {
            items(thumbnails) { thumbnail ->
                ThumbnailItem(bitmap = thumbnail, height = timelineHeight)
            }
        }
        TrimIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .height(timelineHeight)
                .width(trimWidth)
        )
    }

    // LazyRowのスクロール位置からトリム範囲を計算
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val totalOffsetPx = with(density) {
                    index * thumbnailWidth.toPx() + offset
                }
                val trimWidthPx = with(density) {
                    trimWidth.toPx()
                }
                val startMs = (totalOffsetPx / trimWidthPx * 1000).toLong()

                // トリム範囲をコールバック
                onTrimRangeChanged(startMs)
            }
    }
}

/**
 * サムネイルアイテム
 * @param bitmap サムネイル画像
 * @param height 高さ
 */
@Composable
fun ThumbnailItem(bitmap: Bitmap, height: Dp) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Thumbnail",
        modifier = Modifier
            .height(height)
    )
}

/**
 * クロップインジケーター
 * @param modifier Modifier
 */
@Composable
fun TrimIndicator(modifier: Modifier) {
    Box(modifier = modifier.border(2.dp, Color.Yellow))
}

/**
 * サムネイル高さからクロップ幅を計算
 * @param thumbnail サムネイル画像
 * @param targetHeight ターゲット高さ
 */
@Composable
fun calculateTrimWidth(thumbnail: Bitmap, targetHeight: Dp): Dp {
    val density = LocalDensity.current
    val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()

    val targetHeightPx = with(density) { targetHeight.toPx() }
    val newHeightPx = aspectRatio * targetHeightPx
    return with(density) { newHeightPx.toDp() }
}

/**
 * 動画操作ボタン
 * @param onPreview プレビューボタンクリック時のコールバック
 * @param onTrim トリミングボタンクリック時のコールバック
 */
@Composable
fun VideoControlButtons(
    onPreview: () -> Unit,
    onTrim: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onPreview
        ) { Text("Preview") }
        Button(
            onClick = onTrim,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) { Text("Trim") }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoEditScreenPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
//            VideoEditorScreen(rememberNavController())
        }
    }
}
