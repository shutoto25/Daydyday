package com.gmail.shu10.dev.app.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@Composable
fun PlayBackRoute(
    navHostController: NavHostController,
    viewModel: PlayBackViewModel = hiltViewModel(),
) {
    PayBackScreen()
}

@Composable
fun PayBackScreen() {
    Text(text = "再生画面")
}