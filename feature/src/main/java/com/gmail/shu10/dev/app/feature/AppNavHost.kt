package com.gmail.shu10.dev.app.feature

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.diarydetail.DiaryDetailScreen
import com.gmail.shu10.dev.app.feature.diarydetail.DiaryDetailScreenRoute
import com.gmail.shu10.dev.app.feature.main.CustomFloatingAppMenuBar
import com.gmail.shu10.dev.app.feature.main.HomeScreen
import com.gmail.shu10.dev.app.feature.main.HomeScreenRoute
import com.gmail.shu10.dev.app.feature.playback.PlayBackRoute
import com.gmail.shu10.dev.app.feature.setting.SettingScreen
import com.gmail.shu10.dev.app.feature.videoeditor.VideoEditorRoute
import com.gmail.shu10.dev.app.feature.videoeditor.VideoEditorScreenRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val MainGraphRoute = "mainGraph"

/**
 * 画面遷移ホスト
 * @param intent Intent
 * @param viewModel SharedDiaryViewModel
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppNavHost(
    intent: MutableState<Intent?>,
    modifier: Modifier = Modifier,
    viewModel: SharedDiaryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 365) // 初期位置:今日
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()

    // Intentの監視と画面遷移
    LaunchedEffect(intent.value) {
        intent.value?.data?.let {
            val date = it.getQueryParameter("date")
            if (date != null) {
                navController.navigate(DiaryDetailScreenRoute)
            }
        }
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = MainGraphRoute
        ) {
            navigation(startDestination = HomeScreenRoute, route = MainGraphRoute) {
                // ホーム画面
                composable(HomeScreenRoute) { navBackStackEntry ->
                    // コンストラクタのViewModelと同じViewModelStoreOwner(Activity scope)を使って
                    // ViewModelを取得するため同じインスタンスのViewModelが取得できる
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry(MainGraphRoute)
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 16.dp,
                            key = { it }
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    navController = navController,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    gridState = gridState,
                                    navBackStackEntry = parentEntry
                                )
                                1 -> {
                                    // 再生画面
                                    PlayBackRoute(navController)
                                }
                                2 -> {
                                    // 設定画面
                                    SettingScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        // カスタムFloatingAppBarを追加
                        CustomFloatingAppMenuBar(
                            navController = navController,
                            pagerState = pagerState,
                            onHome = {
                                coroutineScope.launch {
                                    if (pagerState.currentPage != 0) {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                            },
                            onPlay = {
                                coroutineScope.launch {
                                    if (pagerState.currentPage != 1) {
                                        pagerState.animateScrollToPage(1)
                                    }
                                }
                            },
                            onSettings = {
                                coroutineScope.launch {
                                    if (pagerState.currentPage != 2) {
                                        pagerState.animateScrollToPage(2)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        )
                    }
                }
                // 日付詳細画面
                composable(DiaryDetailScreenRoute) { navBackStackEntry ->
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry(MainGraphRoute)
                    }
                    DiaryDetailScreen(
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        gridState = gridState,
                        navBackStackEntry = parentEntry
                    )
                }
                // 動画編集画面
                composable(VideoEditorScreenRoute) { navBackStackEntry ->
                    VideoEditorRoute(
                        navController,
                        getDiaryFromNavBackStackEntry(navBackStackEntry)
                    )
                }
            }
        }
    }
}

/**
 * 日記データ取得
 * @param navBackStackEntry NavBackStackEntry
 * @return Diary
 */
private fun getDiaryFromNavBackStackEntry(navBackStackEntry: NavBackStackEntry): Diary {
    val json = navBackStackEntry.arguments?.getString("diaryJson") ?: ""
    val diary = Json.decodeFromString<Diary>(Uri.decode(json))
    return diary
}