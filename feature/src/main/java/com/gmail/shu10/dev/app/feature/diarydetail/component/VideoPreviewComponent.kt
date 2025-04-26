package com.gmail.shu10.dev.app.feature.diarydetail.component

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 動画プレビュー
 * @param uri 動画URI
 */
@Composable
fun VideoPreviewComponent(uri: Uri) {
    val context = LocalContext.current
    val expPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    AndroidView(
        factory = { PlayerView(context).apply { player = expPlayer } },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
    DisposableEffect(Unit) {
        onDispose { expPlayer.release() }
    }
} 