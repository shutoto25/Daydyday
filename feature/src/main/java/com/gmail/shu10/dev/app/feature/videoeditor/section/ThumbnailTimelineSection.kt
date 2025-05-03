package com.gmail.shu10.dev.app.feature.videoeditor.section

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gmail.shu10.dev.app.feature.videoeditor.component.ThumbnailItemComponent
import com.gmail.shu10.dev.app.feature.videoeditor.component.TrimIndicatorComponent

/**
 * サムネイルタイムライン
 * @param thumbnails サムネイルリスト
 */
@Composable
fun ThumbnailTimelineSection(
    thumbnails: List<Bitmap>,
    onTrimRangeChanged: (startMs: Long) -> Unit,
    modifier: Modifier = Modifier
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
                ThumbnailItemComponent(bitmap = thumbnail, height = timelineHeight)
            }
        }
        TrimIndicatorComponent(
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
 * サムネイル高さからクロップ幅を計算
 * @param thumbnail サムネイル画像
 * @param targetHeight ターゲット高さ
 */
@Composable
private fun calculateTrimWidth(thumbnail: Bitmap, targetHeight: Dp): Dp {
    val density = LocalDensity.current
    val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()

    val targetHeightPx = with(density) { targetHeight.toPx() }
    val newHeightPx = aspectRatio * targetHeightPx
    return with(density) { newHeightPx.toDp() }
} 