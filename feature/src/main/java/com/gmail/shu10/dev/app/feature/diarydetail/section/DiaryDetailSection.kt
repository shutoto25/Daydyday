package com.gmail.shu10.dev.app.feature.diarydetail.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.diarydetail.component.MemoComponent

/**
 * 日記詳細セクション
 * @param diary 日記
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onClickAddPhotoOrVideo 写真/動画追加ボタンクリックコールバック
 * @param onClickAddLocation 位置情報追加ボタンクリックコールバック
 * @param modifier Modifier
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiaryDetailSection(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        HeaderSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            date = diary.date
        )
        MediaContentSection(
            diary = diary,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onClickAddPhotoOrVideo = onClickAddPhotoOrVideo,
            onClickAddLocation = onClickAddLocation,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        MemoComponent(
            modifier = Modifier.fillMaxWidth(),
            diary = diary,
            onContentChange = { /* あとで */ }
        )
    }
} 