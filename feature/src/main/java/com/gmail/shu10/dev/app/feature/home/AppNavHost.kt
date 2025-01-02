package com.gmail.shu10.dev.app.feature.home

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * 画面遷移ホスト
 */
@Composable
fun AppNavHost(intent: MutableState<Intent?>) {
    val navController = rememberNavController()

    // Intentの監視と画面遷移
    LaunchedEffect(intent.value) {
        intent.value?.data?.let {
            val date = it.getQueryParameter("date")
            if (date != null) {
                // 日記詳細画面へ遷移
                navController.navigate(AppScreen.Detail(date).route)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home.route
    ) {
        // ホーム画面
        composable(AppScreen.Home.route) { InfiniteDateList(navController) }
        // 日付詳細画面
        composable(AppScreen.Detail("{selectedDate}").route) { navBackStackEntry ->
            val selectedDate = navBackStackEntry.arguments?.getString("selectedDate") ?: ""
            DiaryDetailScreen(navController, selectedDate)
        }
        // 動画編集画面
        composable(AppScreen.VideoEdit.route) { VideoEditScreen(navController) }
    }
}

/**
 * 画面遷移一覧
 */
sealed class AppScreen(val route: String) {
    object Home : AppScreen("home")
    data class Detail(val date: String) : AppScreen("detail/{selectedDate}")
    object VideoEdit : AppScreen("videoEdit")
}

/**
 * 画面遷移ルーティング拡張関数
 */
fun AppScreen.createRoute(): String {
    return when (this) {
        // ホーム画面へ遷移
        is AppScreen.Home -> route
        // 日付詳細画面へ遷移
        is AppScreen.Detail -> route.replace("{selectedDate}", date)
        // 動画編集画面へ遷移
        is AppScreen.VideoEdit -> route
    }
}