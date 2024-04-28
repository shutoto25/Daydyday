package com.gmail.shu10.dev.app.feature.theme

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme

/**
 * Widget用のテーマ
 */
@Composable
fun WidgetTheme(
    content: @Composable () -> Unit,
) {
    GlanceTheme(GlanceTheme.colors) {
        content()
    }
}
