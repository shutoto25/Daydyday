package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gmail.shu10.dev.app.core.utils.DateFormatConstants
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.core.utils.getToday
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.launch

/*
　画面構成方針
　Screen：画面 viewmodelのアクセスはここのみ
　Content：スクリーン直下
　Section：画面幅いっぱいの要素
　Component：セクションより小さな要素
 */

/**
 * ホーム画面(日付リスト)
 * @param navController NavController
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param navBackStackEntry NavBackStackEntry
 * @param viewModel SharedDiaryViewModel
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    navBackStackEntry: NavBackStackEntry,
    viewModel: SharedDiaryViewModel = hiltViewModel(navBackStackEntry),
) {
    val uiState by viewModel.homeUiState.collectAsState()

    HomeContent(
        navController = navController,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        gridState = gridState,
        viewModel = viewModel,
        uiState = uiState,
    )
}

/**
 * ホーム画面コンテンツ
 * @param navController NavController
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param gridState LazyGridState
 * @param viewModel SharedDiaryViewModel
 * @param uiState HomeUiState
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeContent(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    viewModel: SharedDiaryViewModel,
    uiState: HomeUiState,
) {
    ModalNavigationDrawer(
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        drawerContent = { DrawerSection() }
    ) {
        when (uiState) {
            // ローディング
            is HomeUiState.Loading -> LoadingSection()
            // エラー
            is HomeUiState.Error -> ErrorSection(
                message = (uiState as HomeUiState.Error).message,
                onReload = { viewModel.syncDiaryList() }
            )
            // 通常画面
            is HomeUiState.Success -> {
                if (viewModel.getMediaType() == null) {
                    // 初回起動時にメディアタイプを選択
                    MediaTypeComponent { mediaType ->
                        viewModel.setMediaType(mediaType)
                    }
                }

                val context = LocalContext.current
                val (launchCamera, cameraLauncher) = rememberCameraLauncher(
                    context = context,
                    viewModel = viewModel,
                    onPhotoTaken = { photoUri -> }
                )

                val successState = uiState as HomeUiState.Success
                Box(modifier = Modifier.fillMaxSize()) {
                    Column {
                        // 天気情報エリアを追加
                        WeatherInfoSection(
                            weather = WeatherType.SUNNY,
                            temperature = "22°C",
                            location = "現在地",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        ListSection(
                            diaryList = successState.diaryList,
                            gridState = gridState,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onDateClick = { index, diary ->
                                viewModel.selectDiaryEvent(index, diary)
                                navController.navigate(AppScreen.DiaryDetail.route)
                            },
                            onPlay = {
                                navController.navigate(AppScreen.PlayBackRoute.route)
                            },
                            onCamera = {
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * メディアタイプ選択ダイアログ
 * @param onMediaTypeSelected メディアタイプ選択時の処理
 */
@Composable
private fun MediaTypeComponent(
    onMediaTypeSelected: (MediaType) -> Unit,
) {
    // 初回起動時にダイアログを表示するかどうかの状態
    var showDialog by remember { mutableStateOf(true) }

    // ダイアログ表示用のUI
    if (showDialog) {
        val activity = LocalActivity.current
        AlertDialog(
            onDismissRequest = { activity?.finishAffinity() },
            title = { Text("登録方法の選択") },
            text = { Text("写真で日記を登録しますか？それとも動画で登録しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onMediaTypeSelected(MediaType.PHOTO)
                    }
                ) { Text("写真で登録") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onMediaTypeSelected(MediaType.VIDEO)
                    }
                ) { Text("動画で登録") }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false, // 枠外タップ無効
            )
        )
    }
}

/**
 * ドロワー
 */
@Composable
private fun DrawerSection() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        DrawerItemComponent(
            "アカウント(大きめにエリアを取ってトップ位置に表示したい)",
            Icons.Default.Face,
            "アカウント"
        )
        DrawerItemComponent("お知らせ", Icons.Default.Email, "お知らせ")
        DrawerItemComponent("通知", Icons.Default.Notifications, "通知設定")
        DrawerItemComponent("言語", Icons.Default.Settings, "言語設定")
        DrawerItemComponent("ヘルプ", Icons.Default.Star, "ヘルプ")
        DrawerItemComponent("このアプリについて", Icons.Default.Info, "アプリについて")
    }
}

/**
 * ドロワーアイテム
 * @param text テキスト
 * @param icon アイコン
 * @param description 説明
 */
@Composable
private fun DrawerItemComponent(text: String, icon: ImageVector, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = description)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

/**
 * ローディング画面
 */
@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator() // FIXME なぜか表示されない
    }
}

/**
 * エラー画面
 * @param message エラーメッセージ
 * @param onReload リロード処理
 */
@Composable
private fun ErrorSection(message: String, onReload: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "エラーが発生しました")
            Text(text = message)
            Button(onClick = onReload) {
                Text(text = "リトライ")
            }
        }
    }
}

/**
 * 日記リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onDateClick 日付クリック時の処理
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ListSection(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDateClick: (Int, Diary) -> Unit,
    onPlay: () -> Unit,
    onCamera: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetScaffoldState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetMaxHeight = screenHeight * 0.75f
    val sheetSeekHeight = screenHeight * 0.15f
    val density = LocalDensity.current

    // 現在のシートオフセット（dp）を保持する状態
    var currentSheetOffsetDp by remember { mutableStateOf(sheetSeekHeight) }
    // FABのpaddingBottomを計算
    val fabPaddingBottom = screenHeight - currentSheetOffsetDp
    LaunchedEffect(sheetState.bottomSheetState) {
        snapshotFlow {
            try {
                sheetState.bottomSheetState.requireOffset()
            } catch (e: IllegalStateException) {
                // 初回はオフセットが初期化されていない可能性があるため
                with(density) { sheetSeekHeight.toPx() }
            }
        }.collect { currentSheetOffsetDp = with(density) { it.toDp() } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = sheetState,
            sheetPeekHeight = sheetSeekHeight,
            sheetShape = MaterialTheme.shapes.medium,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sheetMaxHeight)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 常に表示されるコンテンツ
                        Text(
                            text = getToday(DateFormatConstants.YYYY_MM_DD_SLASH),
                            fontSize = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sheetSeekHeight - ((22.dp * 2) - 4.dp/*dragHandle分*/))
                                .clickable {
                                    coroutineScope.launch { gridState.animateScrollToItem(index = 365) }
                                },
                        )
                        // 折りたたみ部分のコンテンツ
                        Text(
                            text = "Content",
                        )
                    }
                }
            },
            content = { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    DateGridSection(
                        diaryList = diaryList,
                        gridState = gridState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onDateClick = onDateClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        )
        AnimatedVisibility(
            visible = sheetState.bottomSheetState.targetValue == SheetValue.PartiallyExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { onPlay() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "PLAY"
                        )
                    }
                    FloatingActionButton(
                        onClick = { onCamera() },
                        modifier = Modifier.padding(bottom = fabPaddingBottom)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ADD"
                        )
                    }
                }
            }
        }
    }
}

/**
 * 日付リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onDateClick 日付クリック時の処理
 * @param modifier Modifier
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DateGridSection(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDateClick: (Int, Diary) -> Unit,
    modifier: Modifier = Modifier,
) {
    with(sharedTransitionScope) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = modifier,
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(diaryList) { index, diary ->
                DateGridItemComponent(
                    diary,
                    Modifier.sharedElement(
                        state = rememberSharedContentState(diary.date),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                ) { onDateClick(index, diary) }
            }
        }
    }
}

/**
 * 日付アイテム
 * @param diary 日記
 * @param modifier Modifier
 * @param onClickItem アイテムクリック時の処理
 */
@Composable
private fun DateGridItemComponent(diary: Diary, modifier: Modifier, onClickItem: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // 1:1のアスペクト比(正方形)
            .clickable { onClickItem() }
            .border(
                BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        if (diary.videoPath != null) {
            val context = LocalContext.current
            val thumbnail = remember { getVideoThumbnail(context, diary.videoPath!!.toUri()) }
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Diary's video",
                    contentScale = ContentScale.Crop,
                )
            }
        } else if (diary.photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(diary.photoPath)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(diary.date)
                    .memoryCacheKey(diary.date)
                    .build(),
                modifier = modifier,
                contentDescription = "dairy's photo",
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = convertDateFormat(diary.date),
            Modifier.padding(8.dp),
            fontSize = 16.sp
        )
    }
}

private fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)
        retriever.getFrameAtTime(0) // 1秒目のフレームを取得
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}

@Preview(showBackground = true)
@Composable
private fun InfiniteDateListPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoadingSection()
        }
    }
}