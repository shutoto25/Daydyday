package com.gmail.shu10.dev.app.feature.videoeditor.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button

/**
 * 動画操作ボタンコンポーネント
 * @param text ボタンのテキスト
 * @param onClickButton ボタンクリック時のコールバック
 * @param modifier Modifier
 */
@Composable
fun VideoControlButtonComponent(
    text: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Button(onClick = onClickButton) {
            Text(text)
        }
    }
}