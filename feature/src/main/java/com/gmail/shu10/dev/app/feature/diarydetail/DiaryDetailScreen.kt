package com.gmail.shu10.dev.app.feature.diarydetail

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.SharedDiaryViewModel
import com.gmail.shu10.dev.app.feature.diarydetail.section.DiaryDetailSection
import com.gmail.shu10.dev.app.feature.main.section.ErrorSection
import com.gmail.shu10.dev.app.feature.main.section.LoadingSection
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import com.gmail.shu10.dev.app.feature.videoeditor.navigateToVideoEditorScreen
import kotlinx.coroutines.launch

/**
 * 日記詳細画面のUI状態
 */
sealed class DiaryDetailUiState {
    object Loading : DiaryDetailUiState()

    data class Success(
        val diaryList: List<Diary>,
        val index: Int,
        val diary: Diary?,
    ) : DiaryDetailUiState()

    data class Error(val message: String) : DiaryDetailUiState()
}

/**
 * 日記詳細画面
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiaryDetailScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    navBackStackEntry: NavBackStackEntry,
    viewModel: SharedDiaryViewModel = hiltViewModel(navBackStackEntry),
) {
    val uiState by viewModel.detailUiState.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    DetailContent(
        uiState = uiState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        contentResolver = contentResolver,
        gridState = gridState,
        onSavePhoto = { uri, diary ->
            val file = viewModel.save1secFromPhoto(context, uri, diary.date)
            // キャッシュバスティング用にクエリパラメータを追加
            val newPhotoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            viewModel.updateDiaryListItem((diary.copy(photoPath = newPhotoUri)))
        },
        // TODO 一旦無理くり実装しているので後で修正したい
        onSaveVideo = { uri, diary ->
            viewModel.saveThumbnails(context, uri, diary.date) { thumbnail ->
                val file = viewModel.saveVideoToAppDir(context, uri, diary.date)
                val newVideoUri = file?.toContentUri(context)?.let {
                    "$it?ts=${System.currentTimeMillis()}"
                }
                viewModel.updateDiaryListItem(
                    diary.copy(
                        videoPath = newVideoUri,
                        photoPath = thumbnail?.toContentUri(context)
                            .toString()
                    )
                )
                navController.navigateToVideoEditorScreen(diary.copy(videoPath = newVideoUri))
            }
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailContent(
    uiState: DiaryDetailUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    contentResolver: ContentResolver,
    gridState: LazyGridState,
    onSavePhoto: (Uri, Diary) -> Unit,
    onSaveVideo: (Uri, Diary) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is DiaryDetailUiState.Loading -> {
            LoadingSection(Modifier.fillMaxSize())
        }

        is DiaryDetailUiState.Success -> {
            val pagerState = rememberPagerState(uiState.index) { uiState.diaryList.size }

            // pagerStateとgridStateを同期
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .collect { page ->
                        coroutineScope.launch {
                            // 現在表示されているアイテムの範囲を取得
                            val visibleItems = gridState.layoutInfo.visibleItemsInfo
                            if (visibleItems.isEmpty()) return@launch

                            val firstVisibleIndex = visibleItems.first().index
                            val lastVisibleIndex = visibleItems.last().index

                            // ターゲットページが現在表示されている範囲外の場合のみスクロール
                            if (page < firstVisibleIndex || page > lastVisibleIndex) {
                                // 必要最小限のスクロールを行う
                                val scrollToPosition = when {
                                    page < firstVisibleIndex -> page
                                    else -> maxOf(0, page - visibleItems.size + 1)
                                }
                                gridState.animateScrollToItem(scrollToPosition)
                            }
                        }
                    }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !sharedTransitionScope.isTransitionActive,  // 遷移中は横スクロール抑止
                modifier = Modifier.fillMaxSize()
            ) { page ->
                uiState.diaryList[page].let { diary ->
                    // メディア選択ロジック（画像・動画の選択後の処理）
                    val mediaPickerLauncher = rememberMediaPickerLauncher(
                        contentResolver = contentResolver,
                        onSavePhoto = { uri -> onSavePhoto(uri, diary) },
                        onSaveVideo = { uri -> onSaveVideo(uri, diary) },
                    )

                    DiaryDetailSection(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        diary = diary,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        saveContent = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onClickAddLocation = { /* TODO: 位置情報設定画面へ遷移 */ },
                    )
                }
            }
        }

        is DiaryDetailUiState.Error -> {
            ErrorSection("", onReload = {}, Modifier.fillMaxSize())
        }
    }
}

/**
 * メディア選択ランチャーを生成するComposable
 * 内部で選択されたメディアのMIMEタイプに応じた処理を実行
 */
@Composable
private fun rememberMediaPickerLauncher(
    contentResolver: ContentResolver,
    onSavePhoto: (Uri) -> Unit,
    onSaveVideo: (Uri) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { mediaUri ->
    mediaUri?.let { uri ->
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType?.startsWith("image") == true -> {
                onSavePhoto(uri)
            }

            mimeType?.startsWith("video") == true -> {
                onSaveVideo(uri)
            }
        }
    }
}