package com.gmail.shu10.dev.app.feature.main

import androidx.navigation.NavController

const val HOME_SCREEN_ROUTE = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HOME_SCREEN_ROUTE)
}