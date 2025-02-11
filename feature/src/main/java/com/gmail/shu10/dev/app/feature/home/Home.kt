package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ホーム画面(日付リスト)
 * @param navController NavController
 */
@Composable
fun HomeRoute(
    navController: NavController,
) {
    val viewModel: SharedDiaryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // リスト初期位置は今日
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 365)

    LaunchedEffect(remember { derivedStateOf { gridState.firstVisibleItemIndex } }) {
        viewModel.updateFabState(gridState.firstVisibleItemIndex)
    }

    updateDiaryFromBackStack(navController, viewModel)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent() }
    ) {
        when(uiState) {
            is HomeUiState.Loading -> LoadingScreen()
            is HomeUiState.Error -> ErrorScreen((uiState as HomeUiState.Error).message)
            is HomeUiState.Success -> {
                val successState = uiState as HomeUiState.Success
                HomeScreen(
                    diaryList = successState.diaryList,
                    gridState = gridState,
                    isFabVisible = successState.isFabVisible,
                    fabIcon = successState.fabIcon,
                    onFabClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(index = 365)
                        }
                    },
                    onDateClick = { diary ->
                        navController.navigate(
                            AppScreen.DiaryDetail(Json.encodeToString(diary)).createRoute()
                        )
                    },
                    onPlayClick = {
                        navController.navigate(AppScreen.PlayBackRoute.route)
                    }
                )
            }
        }
    }
}

/**
 * ドロワー
 */
@Composable
fun DrawerContent() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        DrawerContentItem(
            "アカウント(大きめにエリアを取ってトップ位置に表示したい)",
            Icons.Default.Face,
            "アカウント"
        )
        DrawerContentItem("お知らせ", Icons.Default.Email, "お知らせ")
        DrawerContentItem("通知", Icons.Default.Notifications, "通知設定")
        DrawerContentItem("言語", Icons.Default.Settings, "言語設定")
        DrawerContentItem("ヘルプ", Icons.Default.Star, "ヘルプ")
        DrawerContentItem("このアプリについて", Icons.Default.Info, "アプリについて")
    }
}

/**
 * ドロワーアイテム
 * @param text テキスト
 * @param icon アイコン
 * @param description 説明
 */
@Composable
fun DrawerContentItem(text: String, icon: ImageVector, description: String) {
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

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "エラーが発生しました", color = Color.Red)
            Text(text = message)
        }
    }
}

/**
 * ホーム画面コンテンツ
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param isFabVisible FAB表示フラグ
 * @param fabIcon FABアイコン
 * @param onFabClick FABクリック時の処理
 * @param onDateClick 日付クリック時の処理
 */
@Composable
fun HomeScreen(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    isFabVisible: Boolean,
    fabIcon: ImageVector,
    onFabClick: () -> Unit,
    onDateClick: (Diary) -> Unit,
    onPlayClick: () -> Unit,
) {
    Scaffold(
        bottomBar = { BottomNavigationBar(onPlayClick) },
        floatingActionButton = {
            DateFloatingActionButton(
                isFabVisible = isFabVisible,
                icon = fabIcon,
                onClick = onFabClick
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = { innerPadding ->
            DateGrid(
                diaryList = diaryList,
                gridState = gridState,
                onDateClick = onDateClick,
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    )
}

/**
 * ボトムナビゲーションバー
 */
@Composable
fun BottomNavigationBar(onPlayClick: () -> Unit) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        actions = {
            IconButton(onClick = { onPlayClick() }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "再生")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    )
}

/**
 * FABアイコン
 * @param isFabVisible FAB表示フラグ
 * @param icon FABアイコン
 * @param onClick FABクリック時の処理
 */
@Composable
fun DateFloatingActionButton(
    isFabVisible: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isFabVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        FloatingActionButton(
            shape = RoundedCornerShape(50),
            onClick = { onClick() }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "scroll to today's position"
            )
        }
    }
}

/**
 * 日付リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param onDateClick 日付クリック時の処理
 * @param modifier Modifier
 */
@Composable
fun DateGrid(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    onDateClick: (Diary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(diaryList) { diary ->
            DateGridItem(diary) { onDateClick(diary) }
        }
    }
}

/**
 * 日付アイテム
 * @param diary 日記
 * @param onClickItem アイテムクリック時の処理
 */
@Composable
fun DateGridItem(diary: Diary, onClickItem: () -> Unit) {
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
                model = diary.photoPath!!.toUri(),
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

fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
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

/**
 * 日記詳細画面から戻ってきた際に日記データを更新
 * ＠param navController NavController
 * ＠param viewModel HomeViewModel
 */
private fun updateDiaryFromBackStack(navController: NavController, viewModel: SharedDiaryViewModel) {
    val updateDairyJson = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("updateDiary")

    updateDairyJson?.let {
        val updateDairy = Json.decodeFromString<Diary>(it)
//        viewModel.updateDiaryList(updateDairy)
    }
    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.remove<String>("updateDiary")
}

@Preview(showBackground = true)
@Composable
fun InfiniteDateListPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
//            DateGridItem(Diary(date = "2025-01-01"), onClickItem = {})
            DrawerContent()
        }
    }
}