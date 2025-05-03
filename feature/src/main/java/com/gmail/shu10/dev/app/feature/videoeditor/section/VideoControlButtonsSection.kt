package com.gmail.shu10.dev.app.feature.videoeditor.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmail.shu10.dev.app.feature.videoeditor.component.VideoControlButtonComponent

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
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        VideoControlButtonComponent(
            text = "Preview",
            onClickButton = onPreview
        )
        VideoControlButtonComponent(
            text = "Trim",
            onClickButton = onTrim
        )
    }
} 