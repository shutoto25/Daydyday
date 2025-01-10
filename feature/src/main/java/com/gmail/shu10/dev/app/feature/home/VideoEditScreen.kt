package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme

/**
 * 動画編集画面
 * @param navHostController ナビゲーションコントローラー
 */
@Composable
fun VideoEditScreen(
    navHostController: NavHostController,
//    viewModel: DiaryDetailViewModel = hiltViewModel()
) {
    val viewModel: VideoEditViewModel = hiltViewModel()
    // 画面遷移元からの動画URI
    val videoUriString =
        navHostController.previousBackStackEntry?.savedStateHandle?.get<String>("selectedVideoUri")
    val videoUri = videoUriString?.let { Uri.parse(it) }

    val context = LocalContext.current
    // 動画再生プレイヤー
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    // サムネイルリスト
    val thumbnails = remember { viewModel.extractThumbnails(context, videoUri) }

    ViewEditScreenContent(
        context = context,
        exoPlayer = exoPlayer,
        videoUri = videoUri,
        thumbnails = thumbnails
    )
}

/**
 * 動画編集画面コンテンツ
 */
@Composable
fun ViewEditScreenContent(
    context: Context,
    exoPlayer: ExoPlayer,
    videoUri: Uri?,
    thumbnails: List<Bitmap>
) {
    // 動画再生位置
    var currentPosition by remember { mutableLongStateOf(0L) }
    Column {
        VideoPlayer(context = context, exoPlayer = exoPlayer, uri = videoUri)
        ThumbnailTimeline(thumbnails = thumbnails) { newPosition ->
            exoPlayer.seekTo(newPosition)
            currentPosition = newPosition
        }

        VideoSeekBar(
            currentPosition = currentPosition,
            duration = 10000L
        ) { newPosition -> currentPosition = newPosition }

        VideoControlButtons(
            onPreview = {},
            onTrim = {}
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
fun VideoPlayer(context: Context, exoPlayer: ExoPlayer, uri: Uri?) {
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
    }
    // PlayerViewとexpPlayerをAndroidView経由で統合
    AndroidView(
        factory = { PlayerView(context).apply { this.player = exoPlayer } },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}

/**
 * サムネイルタイムライン
 * @param thumbnails サムネイルリスト
 * @param onThumbnailClick サムネイルクリック時のコールバック
 */
@Composable
fun ThumbnailTimeline(
    thumbnails: List<Bitmap>,
    onThumbnailClick: (Long) -> Unit
) {
    val timelineHeight = 80.dp
    val thumbnailWidth = calculateCropWidth(thumbnails[0], timelineHeight)
    val cropWidth = thumbnailWidth * 2 // 1秒分

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(thumbnails) { thumbnail ->
                ThumbnailItem(bitmap = thumbnail, height = timelineHeight)
            }
        }
        // 固定された黄色い枠
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(timelineHeight)
                .width(cropWidth)
                .border(2.dp, Color.Yellow)
        )
    }
}

@Composable
fun calculateCropWidth(thumbnail: Bitmap, targetHeight: Dp): Dp {
    val density = LocalDensity.current
    val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()

    val targetHeightPx = with(density) { targetHeight.toPx() }
    val newWithPx = aspectRatio * targetHeightPx
    return with(density) { newWithPx.toDp() }
}

@Composable
fun ThumbnailItem(bitmap: Bitmap, height: Dp) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Thumbnail",
        modifier = Modifier
            .height(height)
//            .clickable { onThumbnailClick(index * 500L) }
    )
}

/**
 * 動画再生位置スライダー
 * @param currentPosition 現在位置
 * @param duration 動画再生時間
 * @param onPositionChange 位置変更時のコールバック
 */
@Composable
fun VideoSeekBar(
    currentPosition: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit
) {
    Slider(
        value = currentPosition.toFloat(),
        onValueChange = { onPositionChange(it.toLong()) },
        valueRange = 0f..duration.toFloat(),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

/**
 * 動画操作ボタン
 * @param onPreview プレビューボタンクリック時のコールバック
 * @param onTrim トリミングボタンクリック時のコールバック
 */
@Composable
fun VideoControlButtons(onPreview: () -> Unit, onTrim: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(onClick = onPreview) {
            Text("Preview")
        }
        Button(
            onClick = onTrim,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Trim")
        }
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
            VideoEditScreen(rememberNavController())
        }
    }
}
