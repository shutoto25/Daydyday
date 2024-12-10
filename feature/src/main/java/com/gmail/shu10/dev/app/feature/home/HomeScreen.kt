package com.gmail.shu10.dev.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme

/**
 * ホーム画面(日付リスト)
 */
@Composable
fun InfiniteDateList(navController: NavController) {
    val viewModel: HomeViewModel = viewModel()
    val dateList by viewModel.dateList.collectAsState()
    // リスト初期位置は今日
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 365)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(dateList) { date ->
            Text(
                text = date,
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .clickable {
                        navController.navigate(AppScreen.Detail(date).createRoute())
                    }
            )
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