package com.gmail.shu10.dev.app.feature.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
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
    val isProcessing by viewModel.isProcessing

    // 初回のみ実行するよう修正
    LaunchedEffect(Unit) {
        if (!isProcessing && viewModel.mergedVideoUri.value == null) {
            viewModel.mergeVideos(context)
        }
    }

    // 処理状態に応じたUI表示
    Box(modifier = Modifier.fillMaxSize()) {
        if (isProcessing) {
            // 処理中表示
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("動画を準備中...")
            }
        } else if (uri != null) {
            // 動画再生
            Player(context, uri)
        } else {
            // エラー表示
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("動画の準備ができていません")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.mergeVideos(context) }) {
                    Text("再試行")
                }
            }
        }
    }
}

/**
 * 動画再生プレイヤー
 * @param context context
 * @param exoPlayer 動画再生プレイヤー
 * @param uri 動画URI
 */
@OptIn(UnstableApi::class)
@Composable
fun Player(
    context: Context,
    uri: Uri?
) {
    // uri が null の場合は何も表示しない
    if (uri == null) return

    // ExoPlayer の作成
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            try {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true // 自動再生
            } catch (e: Exception) {
            }
        }
    }

    // 画面破棄時に ExoPlayer を解放
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}