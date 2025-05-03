package com.gmail.shu10.dev.app.feature.diarydetail

import androidx.navigation.NavController

const val DIARY_DETAIL_SCREEN_ROUTE = "detail"

fun NavController.navigateToDiaryDetailScreen() {
    navigate(DIARY_DETAIL_SCREEN_ROUTE)
}
