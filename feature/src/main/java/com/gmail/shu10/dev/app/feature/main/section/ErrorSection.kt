package com.gmail.shu10.dev.app.feature.main.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * エラー画面
 * @param message エラーメッセージ
 * @param onReload リロード処理
 */
@Composable
fun ErrorSection(message: String, onReload: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "エラーが発生しました")
            Text(text = message)
            Button(onClick = onReload) {
                Text(text = "リトライ")
            }
        }
    }
} 