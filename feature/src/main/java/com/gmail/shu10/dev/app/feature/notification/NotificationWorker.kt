package com.gmail.shu10.dev.app.feature.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.gmail.shu10.dev.app.core.CoreDrawable
import com.gmail.shu10.dev.app.core.constants.NotificationConstant.DEFAULT_MY_CHANNEL_ID
import com.gmail.shu10.dev.app.core.utils.createDefaultChannel
import com.gmail.shu10.dev.app.core.utils.hasPermission
import com.gmail.shu10.dev.app.feature.home.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 通知を表示するWorker
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val date = inputData.getString("date") ?: return Result.failure()

        if (!hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            return Result.failure()
        }
        showNotification(date)
        return Result.success()
    }

    /**
     * 通知を表示する
     */
    @Suppress("MissingPermission")
    private fun showNotification(date: String) {
        // チャンネル作成
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(createDefaultChannel())

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = Uri.parse("daybyday://diary/detail?date=$date")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 通知作成
        val notification = NotificationCompat.Builder(applicationContext, DEFAULT_MY_CHANNEL_ID)
            .setSmallIcon(CoreDrawable.ic_fcm_notification)
            .setContentTitle("日記を書きましょう")
            .setContentText("日記を書くことで、日々の気づきを振り返ることができます")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(0, notification)
    }
}