package com.gmail.shu10.dev.app.feature.diarydetail.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.diarydetail.component.MemoComponent

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiaryDetailSection(
    tempDiary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        HeaderSection(date = tempDiary.date)
        MediaContentSection(
            diary = tempDiary,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onClickAddPhotoOrVideo = onClickAddPhotoOrVideo,
            onClickAddLocation = onClickAddLocation
        )
        Spacer(modifier = Modifier.height(16.dp))
        MemoComponent(
            modifier = Modifier.fillMaxWidth(),
            diary = tempDiary,
            onContentChange = { /* あとで */ }
        )
    }
} 