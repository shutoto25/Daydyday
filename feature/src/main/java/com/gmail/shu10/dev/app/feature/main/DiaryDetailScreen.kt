package com.gmail.shu10.dev.app.feature.main

import android.content.ContentResolver
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
            val file = viewModel.savePhotoToAppDir(context, uri, diary.date)
            // キャッシュバスティング用にクエリパラメータを追加
            val newPhotoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            viewModel.updateDiaryListItem((diary.copy(photoPath = newPhotoUri)))
        },
        onSaveVideo = { uri, diary ->
            val file = viewModel.saveVideoToAppDir(context, uri, diary.date)
            val newVideoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            viewModel.updateDiaryListItem(diary.copy(videoPath = newVideoUri))
            navController.navigateToVideoEditorScreen(diary.copy(videoPath = newVideoUri))
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
                uiState.diaryList[page].let { diary ->
                    var selectedDiary by remember { mutableStateOf(diary) }
                    // メディア選択ロジック（画像・動画の選択後の処理）
                    val phonePickerLauncher = rememberPhonePickerLauncher(
                        contentResolver = contentResolver,
                        onSavePhoto = { uri -> onSavePhoto(uri, selectedDiary) },
                        onSaveVideo = { uri -> onSaveVideo(uri, selectedDiary) },
                    )

                    DiaryDetailSection(
                        modifier = modifier.fillMaxSize(),
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
            ErrorSection("", onReload = {}, Modifier.fillMaxSize())
        }
    }
}

/**
 * PhonePickerLauncherを生成するComposable
 * 内部で選択されたメディアのMIMEタイプに応じた処理をhandleMediaSelection()に委譲
 */
@Composable
private fun rememberPhonePickerLauncher(
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
            modifier = Modifier.fillMaxWidth(),
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

        diary.videoPath != null || diary.trimmedVideoPath != null -> {
            // 動画パスまたはトリミング済み動画パスがある場合
            val videoUri = diary.trimmedVideoPath?.toUri() ?: diary.videoPath?.toUri()

            MediaPreViewComponent({
                VideoPreviewComponent(videoUri!!)
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
    diary: Diary,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = diary.content,
        onValueChange = onContentChange,
        label = { Text("内容") },
        modifier = modifier,
        maxLines = Int.MAX_VALUE,
        singleLine = false
    )
}