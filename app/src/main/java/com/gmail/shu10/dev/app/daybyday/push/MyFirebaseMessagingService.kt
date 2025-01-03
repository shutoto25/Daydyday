package com.gmail.shu10.dev.app.daybyday.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gmail.shu10.dev.app.core.CoreDrawable
import com.gmail.shu10.dev.app.core.constants.NotificationConstant.DEFAULT_MY_CHANNEL_ID
import com.gmail.shu10.dev.app.core.utils.createDefaultChannel
import com.gmail.shu10.dev.app.core.utils.hasPermission
import com.gmail.shu10.dev.app.feature.home.MainActivity
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
                shouNotification(it.title, it.body, message.data["date"])
            }
        }
    }

    /**
     * 通知表示
     * @param title 通知タイトル
     * @param body 通知本文
     * @param date 日付
     */
    @SuppressLint("MissingPermission") // 権限チェック済み
    private fun shouNotification(title: String?, body: String?, date: String?) {
        // チャンネル作成
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            createDefaultChannel()
        )

        // 通知タップ時のPendingIntent作成
        val pendingIntent = date?.let {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data = Uri.parse("daybyday://diary/detail?date=$date")
            }
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // 通知表示
        val notification = NotificationCompat.Builder(this, DEFAULT_MY_CHANNEL_ID)
            .setSmallIcon(CoreDrawable.ic_fcm_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).notify(0, notification)
    }
}