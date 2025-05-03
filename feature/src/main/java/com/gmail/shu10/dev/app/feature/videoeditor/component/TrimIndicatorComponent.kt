package com.gmail.shu10.dev.app.feature.videoeditor.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * クロップインジケーター
 * @param modifier Modifier
 */
@Composable
fun TrimIndicatorComponent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.border(2.dp, Color.Yellow))
} 