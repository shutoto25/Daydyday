package com.gmail.shu10.dev.app.feature.diarydetail.section

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.core.utils.getDayOfWeek

/**
 * 日付タイトル
 * @param date 日付
 * @param modifier Modifier
 */
@Composable
fun HeaderSection(
    date: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = convertDateFormat(date),
            fontSize = 28.sp
        )
        Text(
            text = getDayOfWeek(date),
            fontSize = 20.sp,
        )
    }
} 