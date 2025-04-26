package com.gmail.shu10.dev.app.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.camera.rememberCameraLauncher
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.main.section.DrawerSection
import com.gmail.shu10.dev.app.feature.main.section.ErrorSection
import com.gmail.shu10.dev.app.feature.main.section.ListSection
import com.gmail.shu10.dev.app.feature.main.section.LoadingSection
import com.gmail.shu10.dev.app.feature.playback.navigateToPlayBackScreen

/*
　画面構成方針
　Screen：画面 viewmodelのアクセスはここのみ
　Content：スクリーン直下
　Section：画面幅いっぱいの要素
　Component：セクションより小さな要素
 */
/**
 * ホーム画面のUI状態
 */
sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(
        val diaryList: List<Diary>,
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

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
        uiState = uiState,
        onReload = { viewModel.syncDiaryList() },
        onSelectDiaryEvent = { index, diary -> viewModel.selectDiaryEvent(index, diary) },
    )
}

/**
 * ホーム画面コンテンツ
 * @param navController NavController
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param gridState LazyGridState
 * @param uiState HomeUiState
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeContent(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyGridState,
    uiState: HomeUiState,
    onReload: () -> Unit,
    onSelectDiaryEvent: (Int, Diary) -> Unit,
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
                message = uiState.message,
                onReload = { onReload }
            )
            // 通常画面
            is HomeUiState.Success -> {
                val context = LocalContext.current
                val (launchCamera, cameraLauncher) = rememberCameraLauncher(
                    context = context,
                    onPhotoTaken = { photoUri -> }
                )

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
                            diaryList = uiState.diaryList,
                            gridState = gridState,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onDateClick = { index, diary ->
                                onSelectDiaryEvent(index, diary)
                                navController.navigateToDiaryDetailScreen()
                            },
                            onPlay = {
                                navController.navigateToPlayBackScreen()
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

@Preview(
    name = "Home Screen - Loading",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_4
)
@Composable
private fun HomeScreenLoadingPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoadingSection()
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    name = "Home Screen - Success",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_4
)
@Composable
private fun HomeScreenSuccessPreview() {
    val mockDiaryList = remember {
        List(10) { index ->
            Diary(
                date = "2024-04-${index + 1}",
                photoPath = if (index % 2 == 0) "test_photo_path" else null,
                videoPath = if (index % 3 == 0) "test_video_path" else null
            )
        }
    }

    DaydydayTheme {
        SharedTransitionLayout {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val gridState = rememberLazyGridState()
                AnimatedVisibility(visible = true) {
                    ListSection(
                        diaryList = mockDiaryList,
                        gridState = gridState,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        onDateClick = { _, _ -> },
                        onPlay = {},
                        onCamera = {}
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Home Screen - Error",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_4
)
@Composable
private fun HomeScreenErrorPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ErrorSection(
                message = "エラーが発生しました",
                onReload = {}
            )
        }
    }
}