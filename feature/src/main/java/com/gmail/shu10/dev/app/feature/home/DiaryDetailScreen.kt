package com.gmail.shu10.dev.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.hilt.navigation.compose.hiltViewModel
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme

/**
 * 詳細ページ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    selectedDate: String,
    viewModel: DiaryViewModel = hiltViewModel()
) {
//    val diary by viewModel.getDiaryById(selectedDate).collectAsState(initial = null)
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Selected Date: $selectedDate",
        )
        TextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("内容") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            maxLines = Int.MAX_VALUE,
            singleLine = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
//            viewModel.saveDiary(diary = Diary(null, content, selectedDate))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("保存")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DateDetailViewPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DateDetailScreen("2025-01-01")
        }
    }
}