package com.gmail.shu10.dev.app.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun PlayBackRoute(
    navHostController: NavHostController,
    viewModel: PlayBackViewModel = hiltViewModel(),
) {
    PayBackScreen(viewModel)
}

@Composable
fun PayBackScreen(viewModel: PlayBackViewModel) {
    val context = LocalContext.current
    viewModel.mergeVideos(context)
    Text(text = "再生画面")
}