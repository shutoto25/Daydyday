package com.gmail.shu10.dev.app.feature.main.section

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.main.component.DateGridItemComponent

/**
 * 日付リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onDateClick 日付クリック時の処理
 * @param modifier Modifier
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DateGridSection(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDateClick: (Int, Diary) -> Unit,
    modifier: Modifier = Modifier,
) {
    with(sharedTransitionScope) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = modifier,
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(diaryList) { index, diary ->
                DateGridItemComponent(
                    diary,
                    Modifier.sharedElement(
                        state = rememberSharedContentState(diary.date),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                ) { onDateClick(index, diary) }
            }
        }
    }
} 