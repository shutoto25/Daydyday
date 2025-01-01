package com.gmail.shu10.dev.app.daybyday.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gmail.shu10.dev.app.core.CoreDrawable
import com.gmail.shu10.dev.app.core.utils.hasPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * firebase cloud messaging サービス
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO サーバ側から通知を送信するために使うのでいずれはどこかに保存しておく
        Log.d(this.javaClass.simpleName, "onNewToken() called with: token = $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(this.javaClass.simpleName, "onMessageReceived() called with: message = $message")

        if (hasPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            message.notification?.let {
                shouNotification(it.title, it.body)
            }
        }
    }

    /**
     * 通知表示
     * @param title 通知タイトル
     * @param body 通知本文
     */
    @SuppressLint("MissingPermission") // 権限チェック済み
    private fun shouNotification(title: String?, body: String?) {
        val channelId = "default_channel_id"
        val channelName = "default channel"

        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(CoreDrawable.ic_fcm_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(0, notification)
    }
}