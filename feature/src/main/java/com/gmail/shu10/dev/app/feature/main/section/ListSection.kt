package com.gmail.shu10.dev.app.feature.main.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.shu10.dev.app.core.utils.DateFormatConstants
import com.gmail.shu10.dev.app.core.utils.getToday
import com.gmail.shu10.dev.app.domain.Diary
import kotlinx.coroutines.launch

/**
 * 日記リスト
 * @param diaryList 日記リスト
 * @param gridState LazyGridState
 * @param sharedTransitionScope SharedTransitionScope
 * @param animatedVisibilityScope AnimatedVisibilityScope
 * @param onDateClick 日付クリック時の処理
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ListSection(
    diaryList: List<Diary>,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDateClick: (Int, Diary) -> Unit,
    onPlay: () -> Unit,
    onCamera: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetScaffoldState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetMaxHeight = screenHeight * 0.75f
    val sheetSeekHeight = screenHeight * 0.15f
    val density = LocalDensity.current

    // 現在のシートオフセット（dp）を保持する状態
    var currentSheetOffsetDp by remember { mutableStateOf(sheetSeekHeight) }
    LaunchedEffect(sheetState.bottomSheetState) {
        snapshotFlow {
            try {
                sheetState.bottomSheetState.requireOffset()
            } catch (e: IllegalStateException) {
                // 初回はオフセットが初期化されていない可能性があるため
                with(density) { sheetSeekHeight.toPx() }
            }
        }.collect { currentSheetOffsetDp = with(density) { it.toDp() } }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // FABのpaddingBottomを計算
        val fabPaddingBottom = this.maxHeight - currentSheetOffsetDp
        BottomSheetScaffold(
            scaffoldState = sheetState,
            sheetPeekHeight = sheetSeekHeight,
            sheetShape = MaterialTheme.shapes.medium,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sheetMaxHeight)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 常に表示されるコンテンツ
                        Text(
                            text = getToday(DateFormatConstants.YYYY_MM_DD_SLASH),
                            fontSize = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(sheetSeekHeight - ((22.dp * 2) - 4.dp/*dragHandle分*/))
                                .clickable {
                                    coroutineScope.launch { gridState.animateScrollToItem(index = 365) }
                                },
                        )
                        // 折りたたみ部分のコンテンツ
                        Text(
                            text = "Content",
                        )
                    }
                }
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    DateGridSection(
                        diaryList = diaryList,
                        gridState = gridState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onDateClick = onDateClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    )
                }
            }
        )
        AnimatedVisibility(
            visible = sheetState.bottomSheetState.targetValue == SheetValue.PartiallyExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { onPlay() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "PLAY"
                        )
                    }
                    FloatingActionButton(
                        onClick = { onCamera() },
                        modifier = Modifier.padding(bottom = fabPaddingBottom)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ADD"
                        )
                    }
                }
            }
        }
    }
} 