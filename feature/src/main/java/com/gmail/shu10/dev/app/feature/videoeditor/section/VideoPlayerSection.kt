package com.gmail.shu10.dev.app.feature.videoeditor.section

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 動画再生プレイヤー
 * @param startPositionMs 再生開始位置
 * @param exoPlayer 動画再生プレイヤー
 * @param uri 動画URI
 * @param modifier 修飾子
 */
@Composable
fun VideoPlayerSection(
    startPositionMs: Long,
    exoPlayer: ExoPlayer?,
    uri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (exoPlayer != null && uri != null) {
        LaunchedEffect(startPositionMs) {
            exoPlayer.seekTo(startPositionMs)
        }
        DisposableEffect(Unit) {
            exoPlayer.apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
            // 画面破棄のタイミングでPlayerを解放
            onDispose { exoPlayer.release() }
        }
        // PlayerViewとexpPlayerをAndroidView経由で統合
        AndroidView(
            modifier = modifier,
            factory = {
                PlayerView(context).apply { this.player = exoPlayer }
            },
        )
    }
} 