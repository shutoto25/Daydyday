package com.gmail.shu10.dev.app.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import com.gmail.shu10.dev.app.core.constants.NotificationConstant.DEFAULT_MY_CHANNEL_ID

/**
 * デフォルトチャンネル作成
 */
fun createDefaultChannel(): NotificationChannel {
    // 通知チャンネルの作成
    val channelId = DEFAULT_MY_CHANNEL_ID
    val channelName = "Push通知"
    val channelDescription = "デフォルトの通知チャンネルです"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val defaultChannel = NotificationChannel(channelId, channelName, importance).apply {
        description = channelDescription
    }
    return defaultChannel
}