package com.gmail.shu10.dev.app.feature.videoeditor.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp

/**
 * サムネイルアイテム
 * @param bitmap サムネイル画像
 * @param height 高さ
 */
@Composable
fun ThumbnailItemComponent(bitmap: Bitmap, height: Dp, modifier: Modifier = Modifier) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Thumbnail",
        modifier = Modifier
            .height(height)
    )
} 