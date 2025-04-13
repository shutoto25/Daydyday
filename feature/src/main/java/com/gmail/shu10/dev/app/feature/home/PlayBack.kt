package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController

/**
 * 動画再生画面
 * @param navHostController NavHostController
 * @param viewModel PlayBackViewModel
 */
@Composable
fun PlayBackRoute(
    navHostController: NavHostController,
    viewModel: PlayBackViewModel = hiltViewModel(),
) {
    // 動画再生プレイヤー
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val uri by viewModel.mergedVideoUri

    PlayBackScreen(viewModel, uri)
}

@Composable
fun PlayBackScreen(viewModel: PlayBackViewModel, uri: Uri?) {
    val context = LocalContext.current
    viewModel.mergeVideos(context)
    Player(context, uri)
}

/**
 * 動画再生プレイヤー
 * @param context context
 * @param exoPlayer 動画再生プレイヤー
 * @param uri 動画URI
 */
@Composable
fun Player(
    context: Context,
    uri: Uri?
) {
    // 画面ごとに新しいプレーヤーを作成
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(uri) {
        uri?.let {
            exoPlayer.apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
        }

        // 画面破棄のタイミングでPlayerを解放
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // PlayerViewとexpPlayerをAndroidView経由で統合
    uri?.let {
        AndroidView(
            factory = { PlayerView(context).apply { player = exoPlayer } },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
    }
}