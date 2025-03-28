package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.core.utils.getDayOfWeek
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 日記詳細画面
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiaryDetailSection(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    navBackStackEntry: NavBackStackEntry,
    viewModel: SharedDiaryViewModel = hiltViewModel(navBackStackEntry),
) {
    /**
     * 画詳細画面のUIスタータス
     */
    val uiState by viewModel.detailUiState.collectAsState()

    DetailContent(
        viewModel = viewModel,
        uiState = uiState,
        navController = navController,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        gridState = gridState,
        onDiaryUpdated = { diary -> viewModel.updateDiaryListItem(diary) }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailContent(
    viewModel: SharedDiaryViewModel, // TODO 渡さなくていいように後ほど変更する
    uiState: DiaryDetailUiState,
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    onDiaryUpdated: (Diary) -> Unit,
) {

    when (uiState) {
        is DiaryDetailUiState.Loading -> {
            TODO()
        }

        is DiaryDetailUiState.Success -> {
            val successState = uiState as DiaryDetailUiState.Success
            val pagerState = rememberPagerState(successState.index) { successState.diaryList.size }

            // pagerStateとgridStateを同期
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .collect { page ->
                        coroutineScope.launch {
                            // 現在表示されているアイテムの範囲を取得
                            val visibleItems = gridState.layoutInfo.visibleItemsInfo
                            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
                            val lastVisible = visibleItems.lastOrNull()?.index ?: 0

                            // ターゲットページが現在表示されている範囲外の場合のみスクロール
                            if (page < firstVisible || page > lastVisible) {
                                // 必要最小限のスクロールを行う
                                val scrollToPosition = when {
                                    page < firstVisible -> page
                                    else -> page - visibleItems.size + 1 // 表示領域の末尾にくるように
                                }
                                gridState.scrollToItem(scrollToPosition)
                            }
                            // 表示範囲内の場合はスクロールしない
                        }
                    }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !sharedTransitionScope.isTransitionActive,  // 遷移中は横スクロール抑止
                modifier = Modifier.fillMaxSize()
            ) { page ->
                successState.diaryList[page].let { diary ->
                    var selectedDiary by remember { mutableStateOf(diary) }
                    // メディア選択ロジック（画像・動画の選択後の処理）
                    val phonePickerLauncher = rememberPhonePickerLauncher(
                        selectedDiary = selectedDiary,
                        viewModel = viewModel,
                        navHostController = navController,
                        onDiaryUpdated = { updatedDiary ->
                            selectedDiary = updatedDiary
                            onDiaryUpdated(updatedDiary)
                        }
                    )

                    DiaryDetailSection(
                        tempDiary = selectedDiary,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClickAddPhotoOrVideo = {
                            phonePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onClickAddLocation = { /* TODO: 位置情報設定画面へ遷移 */ },
                    )
                }
            }
        }

        is DiaryDetailUiState.Error -> {
            // エラー
        }
    }
}

/**
 * PhonePickerLauncherを生成するComposable
 * 内部で選択されたメディアのMIMEタイプに応じた処理をhandleMediaSelection()に委譲
 */
@Composable
private fun rememberPhonePickerLauncher(
    context: Context = LocalContext.current,
    selectedDiary: Diary,
    viewModel: SharedDiaryViewModel,
    navHostController: NavHostController,
    onDiaryUpdated: (Diary) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { mediaUri ->
    mediaUri?.let { uri ->
        handleMediaSelection(
            context = context,
            uri = uri,
            diary = selectedDiary,
            viewModel = viewModel,
            navHostController = navHostController,
            onDiaryUpdated = onDiaryUpdated,
        )
    }
}

/**
 * 選択されたメディアのMIMEタイプに応じた処理を実行
 */
private fun handleMediaSelection(
    context: Context,
    uri: Uri,
    diary: Diary,
    viewModel: SharedDiaryViewModel,
    navHostController: NavHostController,
    onDiaryUpdated: (Diary) -> Unit,
) {
    val mimeType = context.contentResolver.getType(uri) ?: return
    when {
        mimeType.startsWith("image") -> {
            val file = viewModel.savePhotoToAppDir(context, uri, diary.date)
            // キャッシュバスティング用にクエリパラメータを追加
            val newPhotoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            onDiaryUpdated(diary.copy(photoPath = newPhotoUri))
        }

        mimeType.startsWith("video") -> {
            val file = viewModel.saveVideoToAppDir(context, uri, diary.date)
            val newVideoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            onDiaryUpdated(diary.copy(videoPath = newVideoUri))
            navHostController.navigate(
                AppScreen.VideoEditor(
                    Json.encodeToString(diary.copy(videoPath = newVideoUri))
                ).createRoute()
            )
        }

        else -> {
            // 必要なら他のメディアタイプへの処理を追加
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DiaryDetailSection(
    tempDiary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        DateTitleSection(date = tempDiary.date)
        MediaContentSection(
            diary = tempDiary,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onClickAddPhotoOrVideo = { onClickAddPhotoOrVideo() },
            onClickAddLocation = { onClickAddLocation() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiaryContentInput(
            modifier = Modifier,
            diary = tempDiary,
            onContentChange = { /* あとで */ }
        )
    }

}

/**
 * 日付タイトル
 */
@Composable
private fun DateTitleSection(date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp)
    ) {
        Text(
            text = convertDateFormat(date),
            fontSize = 28.sp
        )
        Text(
            text = getDayOfWeek(date),
            fontSize = 20.sp,
        )
    }
}

/**
 * メディア表示
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MediaContentSection(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    when {
        diary.photoPath != null -> {
            MediaPreViewComponent({
                PhotoImageComponent(
                    diary,
                    sharedTransitionScope,
                    animatedVisibilityScope,
                    onClickAddPhotoOrVideo
                )
            }) { onClickAddLocation() }
        }

        diary.trimmedVideoPath != null -> {
            MediaPreViewComponent({
                VideoPreviewComponent(diary.trimmedVideoPath!!.toUri())
            }) { onClickAddLocation() }
        }

        else -> {
            NoMediaViewComponent(onClickAddPhotoOrVideo)
        }
    }
}

@Composable
private fun MediaPreViewComponent(
    content: @Composable () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    content()
    LocationSetting { onClickAddLocation() }
}

/**
 * メディアがない場合のビュー（追加ボタン）
 * @param onClickAddPhotoOrVideo 写真/動画追加ボタンクリックコールバック
 */
@Composable
private fun NoMediaViewComponent(onClickAddPhotoOrVideo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Gray)
            .clickable { onClickAddPhotoOrVideo() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "add",
            modifier = Modifier.size(48.dp)
        )
        Text("写真/動画を追加")
    }
}

/**
 * 位置情報設定
 * @param onClickAddLocation 位置情報追加ボタンクリックコールバック
 */
@Composable
private fun LocationSetting(onClickAddLocation: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClickAddLocation() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "add",
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
        Text("位置情報を追加")
    }
}

/**
 * 写真プレビュー
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PhotoImageComponent(
    diary: Diary,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRefreshClick: () -> Unit,
) {
    with(sharedTransitionScope) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(diary.photoPath)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(diary.date)
                    .memoryCacheKey(diary.date)
                    .build(),
                contentDescription = "dairy's photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedElement(
                        state = rememberSharedContentState(diary.date),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "change",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clickable { onRefreshClick() }
            )
        }
    }
}

/**
 * 動画プレビュー
 * @param uri 動画URI
 */
@Composable
private fun VideoPreviewComponent(uri: Uri) {
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

/**
 * 内容入力欄
 */
@Composable
private fun DiaryContentInput(
    modifier: Modifier,
    diary: Diary,
    onContentChange: (String) -> Unit,
) {
    TextField(
        value = diary.content,
        onValueChange = onContentChange,
        label = { Text("内容") },
        modifier = modifier.fillMaxWidth(),
        maxLines = Int.MAX_VALUE,
        singleLine = false
    )
}

@Preview(showBackground = true)
@Composable
private fun DateDetailViewPreview() {
    DaydydayTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            DateTitleSection(date = "2022-01-01")
            LocationSetting {}
        }
    }
}