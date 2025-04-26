package com.gmail.shu10.dev.app.feature.diarydetail.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.diarydetail.component.LocationSettingComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.NoMediaComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.PhotoComponent
import com.gmail.shu10.dev.app.feature.diarydetail.component.VideoPreviewComponent

/**
 * メディア表示
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaContentSection(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    when {
        diary.photoPath != null -> {
            MediaPreViewComponent({
                PhotoComponent(
                    diary,
                    sharedTransitionScope,
                    animatedVisibilityScope,
                    onClickAddPhotoOrVideo
                )
            }) { onClickAddLocation() }
        }

        diary.videoPath != null || diary.trimmedVideoPath != null -> {
            // 動画パスまたはトリミング済み動画パスがある場合
            val videoUri = diary.trimmedVideoPath?.toUri() ?: diary.videoPath?.toUri()

            MediaPreViewComponent({
                VideoPreviewComponent(videoUri!!)
            }) { onClickAddLocation() }
        }

        else -> {
            NoMediaComponent(onClickAddPhotoOrVideo)
        }
    }
}

@Composable
private fun MediaPreViewComponent(
    content: @Composable () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    content()
    LocationSettingComponent { onClickAddLocation() }
} 