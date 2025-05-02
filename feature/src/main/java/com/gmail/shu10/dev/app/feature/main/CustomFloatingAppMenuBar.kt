package com.gmail.shu10.dev.app.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun CustomFloatingAppMenuBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = {},
    onCamera: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var showBottomBar by remember { mutableStateOf(currentRoute != "detail") }
    var animateBottomBar by remember { mutableStateOf(false) }

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
                // カスタムメニューアイテム
                NavItem(
                    icon = Icons.Rounded.PlayArrow,
                    label = "再生",
                    selected = false,
                    onClick = onPlay
                )
                NavItem(
                    icon = Icons.Rounded.Add,
                    label = "追加",
                    selected = false,
                    onClick = onCamera
                )
                NavItem(
                    icon = Icons.Rounded.Settings,
                    label = "設定",
                    selected = false,
                    onClick = onSettings
                )
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
    }
}

@Preview
@Composable
private fun FloatingAppBarPreview() {
    CustomFloatingAppMenuBar(
        navController = rememberNavController(),
        onPlay = {},
        onCamera = {},
        onSettings = {}
    )
}