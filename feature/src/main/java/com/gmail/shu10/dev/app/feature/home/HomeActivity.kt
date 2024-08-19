package com.gmail.shu10.dev.app.feature.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaydydayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val dateList = homeViewModel.dataList.collectAsState().value
                    DateList(dateList = dateList)
                }
            }
        }
    }
}

@Composable
fun DateList(dateList: List<String>) {
    LazyColumn {
        items(dateList) { date ->
            Text(text = date)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DaydydayTheme {
        DateList(dateList = listOf("2024-08-18", "2024-08-19", "2024-08-20"))
    }
}