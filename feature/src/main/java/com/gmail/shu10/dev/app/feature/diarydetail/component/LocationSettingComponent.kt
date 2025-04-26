package com.gmail.shu10.dev.app.feature.diarydetail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 位置情報設定
 * @param onClickAddLocation 位置情報追加ボタンクリックコールバック
 * @param modifier Modifier
 */
@Composable
fun LocationSettingComponent(
    onClickAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onClickAddLocation() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "add",
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
        Text("位置情報を追加")
    }
} 