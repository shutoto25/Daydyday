package com.gmail.shu10.dev.app.feature.widget

import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import com.gmail.shu10.dev.app.core.CoreString
import com.gmail.shu10.dev.app.feature.theme.WidgetTheme

/**
 * Widget本体に関する処理
 */
class MyGlanceAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(id = CoreString.hello_glance))
                }
            }
        }
    }
}