package com.gmail.shu10.dev.app.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import java.util.UUID

/**
 * 詳細ページ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    selectedDate: String, viewModel: DiaryViewModel = hiltViewModel()
) {
    // FlowをcollectAsStateで監視
    val diary by viewModel.getDiaryByDate(selectedDate).collectAsState(initial = null)
    // 状態管理
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    // 状態監視
    LaunchedEffect(diary) {
        title = diary?.title ?: ""
        content = diary?.content ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Selected Date: $selectedDate",
        )
        DiaryTitleInput(
            title = title,
            onTitleChange = { title = it }
        )
        DiaryActionButtons(
            onAddPhotoOrVideo = { /* TODO: 写真/動画を追加 */ },
            onAddLocation = { /* TODO: 位置情報を追加 */ }
        )
        DiaryContentInput(
            modifier = Modifier.weight(1f),
            content = content,
            onContentChange = { content = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DiarySaveButton(
            onSave = {
                val saveData = Diary(
                    id = diary?.id,
                    uuid = diary?.uuid ?: UUID.randomUUID().toString(),
                    date = selectedDate,
                    title = title,
                    content = content,
                    photoPath = null,
                    videoPath = null,
                    location = null,
                    isSynced = false
                )
                viewModel.saveDiary(saveData)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryTitleInput(title: String, onTitleChange: (String) -> Unit) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("タイトル") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = Int.MAX_VALUE,
        singleLine = true
    )
}

@Composable
fun DiaryActionButtons(
    onAddPhotoOrVideo: () -> Unit,
    onAddLocation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(4.dp),
            onClick = onAddPhotoOrVideo
        ) {
            Text("写真/動画を追加")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(4.dp),
            onClick = onAddLocation
        ) {
            Text("位置情報を追加")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryContentInput(
    modifier: Modifier,
    content: String,
    onContentChange: (String) -> Unit
) {
    TextField(
        value = content,
        onValueChange = onContentChange,
        label = { Text("内容") },
        modifier = modifier.fillMaxWidth(),
        maxLines = Int.MAX_VALUE,
        singleLine = false
    )
}

@Composable
fun DiarySaveButton(onSave: () -> Unit) {
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("保存")
    }
}

private fun createSaveData(diary: Diary) {

}

@Preview(showBackground = true)
@Composable
fun DateDetailViewPreview() {
    DaydydayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DiaryDetailScreen("2025-01-01")
        }
    }
}

