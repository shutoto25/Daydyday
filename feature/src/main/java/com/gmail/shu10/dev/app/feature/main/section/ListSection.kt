package com.gmail.shu10.dev.app.feature.main.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.shu10.dev.app.core.utils.DateFormatConstants
import com.gmail.shu10.dev.app.core.utils.getToday
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.main.CustomFloatingAppMenuBar
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * 日記リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onDateClick 日付クリック時の処理
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun ListSection(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDateClick: (Int, Diary) -> Unit,
    onPlay: () -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = getToday(DateFormatConstants.YYYY_MM_DD_SLASH),
                            fontSize = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        DateGridSection(
                            diaryList = diaryList,
                            gridState = gridState,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onDateClick = onDateClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        )
                    }
                }
                1 -> {
                    // 再生画面
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "再生画面",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                2 -> {
                    // 設定画面
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "設定画面",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // カスタムFloatingAppBarを追加
        CustomFloatingAppMenuBar(
            navController = rememberNavController(),
            pagerState = pagerState,
            onHome = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            },
            onPlay = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            },
            onSettings = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(2)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
} 