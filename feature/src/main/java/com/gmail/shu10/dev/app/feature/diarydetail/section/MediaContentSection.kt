package com.gmail.shu10.dev.app.feature.diarydetail.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.diarydetail.component.LocationSettingComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.NoMediaComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.PhotoComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.VideoPreviewComponent

/**
 * メディア表示
 * @param diary 日記
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onClickAddPhotoOrVideo 写真/動画追加ボタンクリックコールバック
 * @param onClickAddLocation 位置情報追加ボタンクリックコールバック
 * @param modifier Modifier
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaContentSection(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        diary.photoPath != null -> {
            MediaPreViewComponent(
                modifier = modifier,
                content = {
                    PhotoComponent(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        diary = diary,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onRefreshClick = onClickAddPhotoOrVideo
                    )
                },
                onClickAddLocation = onClickAddLocation
            )
        }

        diary.videoPath != null || diary.trimmedVideoPath != null -> {
            // 動画パスまたはトリミング済み動画パスがある場合
            val videoUri = diary.trimmedVideoPath?.toUri() ?: diary.videoPath?.toUri()

            MediaPreViewComponent(
                modifier = modifier,
                content = {
                    VideoPreviewComponent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        uri = videoUri!!
                    )
                },
                onClickAddLocation = onClickAddLocation
            )
        }

        else -> {
            NoMediaComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClickAddPhotoOrVideo = onClickAddPhotoOrVideo
            )
        }
    }
}

@Composable
private fun MediaPreViewComponent(
    content: @Composable () -> Unit,
    onClickAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    content()
    LocationSettingComponent(
        modifier = modifier,
        onClickAddLocation = onClickAddLocation
    )
} 