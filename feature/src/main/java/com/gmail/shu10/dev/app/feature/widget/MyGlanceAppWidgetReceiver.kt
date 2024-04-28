package com.gmail.shu10.dev.app.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Widget更新時レシーバー
 */
class MyGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {

    // 実際に表示するUIを定義したGlanceAppWidgetを返す
    override val glanceAppWidget: GlanceAppWidget = MyGlanceAppWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 今回はstatelessなため画面更新は不要、特別な対応なし
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}