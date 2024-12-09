package com.gmail.shu10.dev.app.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun InfiniteDateList(viewModel: HomeViewModel = viewModel()) {
    val dateList by viewModel.dateList.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(dateList) { date ->
            Text(
                text = date,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == 0 && offset <= 10) {
                    viewModel.loadMoreDateList(ScrollDirection.UP)
                } else if (index >= dateList.size - 1) {
                    viewModel.loadMoreDateList(ScrollDirection.DOWN)
                }
            }
    }
//    LaunchedEffect(listState) {
//        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
//            .distinctUntilChanged()
//            .collect { visibleItems ->
//                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: 0
//                val firstVisibleItemIndex = visibleItems.firstOrNull()?.index ?: 0
//                if (firstVisibleItemIndex == 0) {
//                    viewModel.loadMoreDateList(ScrollDirection.UP)
//                } else if (lastVisibleItemIndex >= dateList.size - 1) {
//                    viewModel.loadMoreDateList(ScrollDirection.DOWN)
//                }
//            }
//    }
}

@Preview(showBackground = true)
@Composable
fun InfiniteDateListPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InfiniteDateList(viewModel = PreviewViewModel())
        }
    }
}

class PreviewViewModel : HomeViewModel() {
    override val dateList: StateFlow<List<String>>
        get() = MutableStateFlow(listOf("2024-08-20", "2024-08-21"))
}