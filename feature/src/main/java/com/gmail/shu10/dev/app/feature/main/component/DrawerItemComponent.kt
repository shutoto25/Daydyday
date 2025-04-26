package com.gmail.shu10.dev.app.feature.main.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * ドロワーアイテム
 * @param text テキスト
 * @param icon アイコン
 * @param description 説明
 */
@Composable
fun DrawerItemComponent(text: String, icon: ImageVector, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = description)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
} 