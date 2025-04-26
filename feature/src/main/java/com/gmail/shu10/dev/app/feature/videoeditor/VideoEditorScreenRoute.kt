package com.gmail.shu10.dev.app.feature.videoeditor

import android.net.Uri
import androidx.navigation.NavController
import com.gmail.shu10.dev.app.domain.Diary
import kotlinx.serialization.json.Json

const val VideoEditorScreenRoute = "videoEditor/{diaryJson}"

fun NavController.navigateToVideoEditorScreen(diary: Diary) {
    val json = Json.encodeToString(Diary.serializer(), diary)
    navigate("videoEditor/${Uri.encode(json)}")
}