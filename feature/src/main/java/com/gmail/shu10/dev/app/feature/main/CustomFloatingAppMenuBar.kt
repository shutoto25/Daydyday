package com.gmail.shu10.dev.app.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class TabItem(val icon: ImageVector, val label: String) {
    object Home : TabItem(Icons.Rounded.Home, "ホーム")
    object Play : TabItem(Icons.Rounded.PlayArrow, "再生")
    object Settings : TabItem(Icons.Rounded.Settings, "設定")

    companion object {
        fun tabs() = listOf(Home, Play, Settings)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomFloatingAppMenuBar(
    navController: NavController,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onHome: () -> Unit = {},
    onPlay: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var showBottomBar by remember { mutableStateOf(currentRoute != "detail") }
    var animateBottomBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        if (currentRoute == "detail") {
            showBottomBar = false
            animateBottomBar = false
        } else {
            showBottomBar = true
            delay(500)
            animateBottomBar = true
        }
    }

    // ページが変更されたときにコールバックを実行
    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> onHome()
            1 -> onPlay()
            2 -> onSettings()
        }
    }

    AnimatedVisibility(
        visible = showBottomBar && animateBottomBar,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem.tabs().forEachIndexed { index, tab ->
                    NavItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        val transition = updateTransition(targetState = selected, label = "NavItemTransition")

        val iconColor by transition.animateColor(label = "IconColor") { isSelected ->
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = iconColor
        )
    }
}

@Preview
@Composable
private fun FloatingAppBarPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { TabItem.tabs().size }
    CustomFloatingAppMenuBar(
        navController = rememberNavController(),
        pagerState = pagerState,
        onHome = {},
        onPlay = {},
        onSettings = {}
    )
}