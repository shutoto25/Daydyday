package com.gmail.shu10.dev.app.feature.diarydetail.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gmail.shu10.dev.app.domain.Diary

/**
 * 写真プレビュー
 * @param diary 日記
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onClickChange 写真/動画追加ボタンクリックコールバック
 * @param modifier Modifier
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoComponent(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Box(modifier = modifier) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(diary.photoPath)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(diary.date)
                    .memoryCacheKey(diary.date)
                    .build(),
                contentDescription = "日記の写真",
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedElement(
                        state = rememberSharedContentState(diary.date),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "写真を変更",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clickable { onClickChange() }
            )
        }
    }
} 