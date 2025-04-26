package com.gmail.shu10.dev.app.feature.main.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.shu10.dev.app.feature.main.component.DrawerItemComponent

/**
 * ドロワー
 */
@Composable
fun DrawerSection() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        DrawerItemComponent(
            "アカウント(大きめにエリアを取ってトップ位置に表示したい)",
            Icons.Default.Face,
            "アカウント"
        )
        DrawerItemComponent("お知らせ", Icons.Default.Email, "お知らせ")
        DrawerItemComponent("通知", Icons.Default.Notifications, "通知設定")
        DrawerItemComponent("言語", Icons.Default.Settings, "言語設定")
        DrawerItemComponent("ヘルプ", Icons.Default.Star, "ヘルプ")
        DrawerItemComponent("このアプリについて", Icons.Default.Info, "アプリについて")
    }
} 