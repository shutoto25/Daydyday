package com.gmail.shu10.dev.app.feature.notification

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleNotification(context: Context, date: String, tileInMilliSeconds: Long) {
    val inputData = Data.Builder().putString("date", date).build()

    val delay = tileInMilliSeconds - System.currentTimeMillis()

    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInputData(inputData)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}