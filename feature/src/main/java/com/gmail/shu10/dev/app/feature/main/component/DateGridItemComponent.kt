package com.gmail.shu10.dev.app.feature.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gmail.shu10.dev.app.core.utils.getDay
import com.gmail.shu10.dev.app.core.utils.getMonth
import com.gmail.shu10.dev.app.core.utils.getMonthName
import com.gmail.shu10.dev.app.core.utils.getYear
import com.gmail.shu10.dev.app.domain.Diary

/**
 * 日付アイテム
 * @param diary 日記
 * @param modifier Modifier
 * @param onClickItem アイテムクリック時の処理
 */
@Composable
fun DateGridItemComponent(
    diary: Diary,
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f) // 1:1のアスペクト比(正方形)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClickItem() },
        contentAlignment = Alignment.Center
    ) {
        if (diary.photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(diary.photoPath)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(diary.date)
                    .memoryCacheKey(diary.date)
                    .build(),
                modifier = modifier,
                contentDescription = "dairy's photo",
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = formatDisplayDate(diary.date),
                Modifier.padding(8.dp),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 表示する日付を判定する
 * @param date yyyy-MM-dd形式の日付
 * @return 表示用の日付文字列
 */
private fun formatDisplayDate(date: String): String {
    val year = getYear(date)
    val month = getMonth(date)
    val day = getDay(date)

    // 1/1の場合は年を表示
    // x/1の場合は月を表示
    // それ以外は日を表示
    return when {
        month == 1 && day == 1 -> year
        day == 1 -> getMonthName(date)
        else -> day
    }.toString()
} 