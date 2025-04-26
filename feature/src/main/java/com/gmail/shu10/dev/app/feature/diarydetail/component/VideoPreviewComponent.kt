package com.gmail.shu10.dev.app.feature.diarydetail.component

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 動画プレビュー
 * @param uri 動画URI
 * @param modifier Modifier
 */
@Composable
fun VideoPreviewComponent(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { PlayerView(context).apply { player = expPlayer } }
    )
    DisposableEffect(Unit) {
        onDispose { expPlayer.release() }
    }
} 