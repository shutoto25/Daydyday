package com.gmail.shu10.dev.app.feature.main

import androidx.navigation.NavController

const val DiaryDetailScreenRoute = "detail"

fun NavController.navigateToDiaryDetailScreen() {
    navigate(DiaryDetailScreenRoute)
}