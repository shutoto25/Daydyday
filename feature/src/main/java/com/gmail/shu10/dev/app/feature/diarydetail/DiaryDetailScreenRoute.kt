package com.gmail.shu10.dev.app.feature.diarydetail

import androidx.navigation.NavController

const val DiaryDetailScreenRoute = "detail"

fun NavController.navigateToDiaryDetailScreen() {
    navigate(DiaryDetailScreenRoute)
}