package com.gmail.shu10.dev.app.feature.videoeditor.section

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 動画再生プレイヤー
 * @param context context
 * @param exoPlayer 動画再生プレイヤー
 * @param uri 動画URI
 */
@Composable
fun VideoPlayerSection(
    context: Context,
    startPositionMs: Long,
    exoPlayer: ExoPlayer,
    uri: Uri?,
    modifier: Modifier = Modifier
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