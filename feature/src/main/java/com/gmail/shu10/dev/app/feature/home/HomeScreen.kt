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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 日付リスト
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
                        navController.navigate("dateDetail/$date")
                    }
            )
        }
    }
}

/**
 * 画面遷移ホスト
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dateList"
    ) {
        composable("dateList") {
            InfiniteDateList(navController)
        }

        composable("dateDetail/{selectedDate}") { navBackStackEntry ->
            val selectedDate = navBackStackEntry.arguments?.getString("selectedDate") ?: ""
            DateDetailView(selectedDate)
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
//            InfiniteDateList(viewModel = PreviewViewModel())
            AppNavHost()
        }
    }
}

class PreviewViewModel : HomeViewModel() {
    override val dateList: StateFlow<List<String>>
        get() = MutableStateFlow(listOf("2024-08-20", "2024-08-21"))
}