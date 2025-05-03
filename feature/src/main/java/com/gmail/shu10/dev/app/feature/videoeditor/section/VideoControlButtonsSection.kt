package com.gmail.shu10.dev.app.feature.videoeditor.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 動画操作ボタン
 * @param onPreview プレビューボタンクリック時のコールバック
 * @param onTrim トリミングボタンクリック時のコールバック
 */
@Composable
fun VideoControlButtonsSection(
    onPreview: () -> Unit,
    onTrim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onPreview
        ) { Text("Preview") }
        Button(
            onClick = onTrim,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) { Text("Trim") }
    }
} 