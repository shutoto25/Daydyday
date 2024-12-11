package com.gmail.shu10.dev.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.launch

/**
 * ホーム画面(日付リスト)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteDateList(navController: NavController) {
    val viewModel: MainViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val dateList by viewModel.dateList.collectAsState()
    // リスト初期位置は今日
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 365)
    // FAB表示フラグ（今日に近い場合はFABを表示しない）
    val isFabVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex != 365 }
    }
    // FABアイコン（今日を基準に過去は下向きアイコン、未来は上向きアイコンを設定）
    val fabIcon by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex < 365)
                Icons.Default.KeyboardArrowDown
            else if (listState.firstVisibleItemIndex > 365)
                Icons.Default.KeyboardArrowUp
            else null
        }
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                FloatingActionButton(
                    shape = RoundedCornerShape(50),
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index = 365)
                        }
                    }) {
                    fabIcon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = "scroll to today's position"
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(dateList) { date ->
                Text(
                    text = date,
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .clickable {
                            navController.navigate(
                                AppScreen
                                    .Detail(date)
                                    .createRoute()
                            )
                        }
                )
            }
        }
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
            AppNavHost()
        }
    }
}