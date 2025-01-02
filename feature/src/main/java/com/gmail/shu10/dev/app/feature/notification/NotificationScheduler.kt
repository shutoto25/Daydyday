package com.gmail.shu10.dev.app.feature.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 通知スケジューラ
 */
fun scheduleNotification(context: Context) {
    val workManager = WorkManager.getInstance(context)

    val currentTime = System.currentTimeMillis()
    val targetTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val delay = if (currentTime > targetTime) {
        targetTime + TimeUnit.DAYS.toMillis(1) - currentTime
    } else {
        targetTime - currentTime
    }
    // WorkRequestを作成
    val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    // Workerをスケジュール
    workManager.enqueueUniquePeriodicWork(
        "notification",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}