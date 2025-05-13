package com.gmail.shu10.dev.app.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * タブアイテムを表すシールドクラス
 * @property icon タブのアイコン
 * @property label タブのラベル
 */
sealed class TabItem(val icon: ImageVector, val label: String) {
    /**
     * ホームタブ
     */
    object Home : TabItem(Icons.Default.Home, "ホーム")

    /**
     * 再生タブ
     */
    object Play : TabItem(Icons.Default.PlayArrow, "再生")

    /**
     * 設定タブ
     */
    object Settings : TabItem(Icons.Default.Settings, "設定")

    companion object {
        /**
         * 全タブのリストを取得
         * @return タブのリスト
         */
        fun tabs() = listOf(Home, Play, Settings)
    }
}

/**
 * カスタムフローティングアプリメニューバー
 * 画面下部に表示されるナビゲーションメニュー
 * @param navController ナビゲーションコントローラー
 * @param pagerState ページャーの状態
 * @param modifier モディファイア
 * @param onHome ホームタブが選択された時のコールバック
 * @param onPlay 再生タブが選択された時のコールバック
 * @param onSettings 設定タブが選択された時のコールバック
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomFloatingAppMenuBar(
    navController: NavController,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onHome: () -> Unit = {},
    onPlay: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var showBottomBar by remember { mutableStateOf(currentRoute != "detail") }
    var animateBottomBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        if (currentRoute == "detail") {
            showBottomBar = false
            animateBottomBar = false
        } else {
            showBottomBar = true
            delay(500)
            animateBottomBar = true
        }
    }

    // ページが変更されたときにコールバックを実行
    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> onHome()
            1 -> onPlay()
            2 -> onSettings()
        }
    }

    AnimatedVisibility(
        visible = showBottomBar && animateBottomBar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "ホーム",
                        tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "再生",
                        tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FloatingAppBarPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { TabItem.tabs().size }
    CustomFloatingAppMenuBar(
        navController = rememberNavController(),
        pagerState = pagerState,
        onHome = {},
        onPlay = {},
        onSettings = {}
    )
}