package com.gmail.shu10.dev.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.launch

/**
 * ホーム画面(日付リスト)
 */
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val dateList by viewModel.dateList.collectAsState()
    // リスト初期位置は今日
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 365)
    // FAB表示フラグ（今日に近い場合はFABを表示しない）
    val isFabVisible by remember { derivedStateOf { gridState.firstVisibleItemIndex !in 360..365 } }
    // FABアイコン（今日を基準に過去は下向きアイコン、未来は上向きアイコンを設定）
    val fabIcon by remember {
        derivedStateOf {
            when {
                gridState.firstVisibleItemIndex <= 365 -> Icons.Default.KeyboardArrowDown
                gridState.firstVisibleItemIndex > 365 -> Icons.Default.KeyboardArrowUp
                else -> Icons.Default.KeyboardArrowDown // ならない想定なのでデフォルト下向きアイコン設定
            }
        }
    }

    HomeScreenContent(
        dateList = dateList,
        gridState = gridState,
        isFabVisible = isFabVisible,
        fabIcon = fabIcon,
        onFabClick = {
            coroutineScope.launch {
                gridState.animateScrollToItem(index = 365)
            }
        },
        onDateClick = { date ->
            navController.navigate(AppScreen.DiaryDetail(date).createRoute())
        }
    )
}

/**
 * ホーム画面コンテンツ
 * @param dateList 日付リスト
 * @param gridState LazyGridState
 * @param isFabVisible FAB表示フラグ
 * @param fabIcon FABアイコン
 * @param onFabClick FABクリック時の処理
 * @param onDateClick 日付クリック時の処理
 */
@Composable
fun HomeScreenContent(
    dateList: List<String>,
    gridState: LazyGridState,
    isFabVisible: Boolean,
    fabIcon: ImageVector,
    onFabClick: () -> Unit,
    onDateClick: (String) -> Unit
) {
    Scaffold(floatingActionButton = {
        DateFloatingActionButton(
            isFabVisible = isFabVisible,
            icon = fabIcon,
            onClick = onFabClick
        )
    },
        floatingActionButtonPosition = FabPosition.Center,
        content = { innerPadding ->
            DateGrid(
                dateList = dateList,
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
 * FABアイコン
 * @param isFabVisible FAB表示フラグ
 * @param icon FABアイコン
 * @param onClick FABクリック時の処理
 */
@Composable
fun DateFloatingActionButton(
    isFabVisible: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
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
 * @param dateList 日付リスト
 * @param gridState LazyGridState
 * @param onDateClick 日付クリック時の処理
 * @param modifier Modifier
 */
@Composable
fun DateGrid(
    dateList: List<String>,
    gridState: LazyGridState,
    onDateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dateList) { date ->
            DateGridItem(date) { onDateClick(date) }
        }
    }
}

/**
 * 日付アイテム
 * @param date 日付
 * @param onClickItem アイテムクリック時の処理
 */
@Composable
fun DateGridItem(date: String, onClickItem: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable { onClickItem() }
            .size(120.dp)
            .border(
                BorderStroke(2.dp, Color.Gray),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp)
    ) {
        Text(text = convertDateFormat(date), Modifier.padding(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun InfiniteDateListPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DateGridItem("2022-01-01", onClickItem = {})
        }
    }
}