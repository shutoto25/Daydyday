package com.gmail.shu10.dev.app.feature.diarydetail.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmail.shu10.dev.app.domain.Diary

/**
 * 内容入力欄
 * @param diary 日記
 * @param onContentChange 内容変更コールバック
 * @param modifier Modifier
 */
@Composable
fun MemoComponent(
    diary: Diary,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        modifier = modifier,
        value = diary.content,
        onValueChange = onContentChange,
        label = { Text("内容") },
        maxLines = Int.MAX_VALUE,
        singleLine = false
    )
} 