package com.gmail.shu10.dev.app.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.gmail.shu10.dev.app.domain.Diary
import kotlinx.serialization.json.Json

/**
 * 画面遷移ホスト
 * @param intent Intent
 * @param viewModel SharedDiaryViewModel
 */
@Composable
fun AppNavHost(
    intent: MutableState<Intent?>,
    viewModel: SharedDiaryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Intentの監視と画面遷移
    // TODO: dateパラメータ使ってないな
    LaunchedEffect(intent.value) {
        intent.value?.data?.let {
            val date = it.getQueryParameter("date")
            if (date != null) {
                // 日記詳細画面へ遷移
                viewModel.setTodayDiary()
                navController.navigate(AppScreen.DiaryDetail.route)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "mainGraph"
    ) {
        navigation(startDestination = AppScreen.Home.route, route = "mainGraph") {
            // ホーム画面
            composable(AppScreen.Home.route) { navBackStackEntry ->
                // コンストラクタのViewModelと同じViewModelStoreOwner(Activity scope)を使って
                // ViewModelを取得するため同じインスタンスのViewModelが取得できる
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }
                HomeRoute(navController, parentEntry)
            }
            // 日付詳細画面
            composable(AppScreen.DiaryDetail.route) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry("mainGraph")
                }
                DiaryDetailRoute(navController, parentEntry)
            }
            // 動画編集画面
            composable(AppScreen.VideoEditor("{diaryJson}").route) { navBackStackEntry ->
                VideoEditorScreen(navController, getDiaryFromNavBackStackEntry(navBackStackEntry))
            }
            // 再生画面
            composable(AppScreen.PlayBackRoute.route) { PlayBackRoute(navController) }
        }
    }
}

/**
 * 日記データ取得
 * @param navBackStackEntry NavBackStackEntry
 * @return Diary
 */
private fun getDiaryFromNavBackStackEntry(navBackStackEntry: NavBackStackEntry): Diary {
    val json = navBackStackEntry.arguments?.getString("diaryJson") ?: ""
    val diary = Json.decodeFromString<Diary>(Uri.decode(json))
    return diary
}

/**
 * 画面遷移一覧
 */
sealed class AppScreen(val route: String) {
    data object Home : AppScreen("home")
    data object DiaryDetail : AppScreen("detail")
    data class VideoEditor(val diaryJson: String) : AppScreen("videoEditor/{diaryJson}")
    data object PlayBackRoute : AppScreen("playBack")
}

/**
 * 画面遷移ルーティング拡張関数
 */
fun AppScreen.createRoute(): String {
    return when (this) {
        // ホーム画面へ遷移
        is AppScreen.Home -> route
        // 日付詳細画面へ遷移
        is AppScreen.DiaryDetail -> route
        // 動画編集画面へ遷移
        is AppScreen.VideoEditor -> "videoEditor/${Uri.encode(diaryJson)}"
        // 再生画面へ遷移
        is AppScreen.PlayBackRoute -> route
    }
}