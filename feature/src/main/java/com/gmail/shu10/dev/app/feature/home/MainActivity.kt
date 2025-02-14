package com.gmail.shu10.dev.app.feature.home

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.gmail.shu10.dev.app.feature.notification.scheduleDailyNotification
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 画面基盤
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Intentの状態管理
    private val currentIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent.value = intent
        // 画面コンテンツ設定
        setContentValue()
        // デイリー通知設定
        scheduleDailyNotification(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Recent Apps から起動されたとき
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            return
        }
        currentIntent.value = intent
    }

    /**
     * 画面コンテンツ設定
     */
    private fun setContentValue() {
        setContent {
            DaydydayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(currentIntent)
                    PermissionRequestDialog(this, Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}