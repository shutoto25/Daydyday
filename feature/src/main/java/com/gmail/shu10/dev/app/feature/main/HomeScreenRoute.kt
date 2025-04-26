package com.gmail.shu10.dev.app.feature.main

import androidx.navigation.NavController

const val HomeScreenRoute = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HomeScreenRoute)
} 