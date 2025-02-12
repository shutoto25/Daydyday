package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.core.utils.getDayOfWeek
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * 日記詳細画面
 */
@Composable
fun DiaryDetailScreen(
    navHostController: NavHostController,
    navBackStackEntry: NavBackStackEntry,
    diary: Diary,
    viewModel: SharedDiaryViewModel = hiltViewModel(navBackStackEntry)
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    // 状態管理
    var tempDiary by remember { mutableStateOf(diary) }

    val updateDairyJson = navHostController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("updateDiaryWithTrimmedVideo")

    updateDairyJson?.let {
        tempDiary = Json.decodeFromString<Diary>(it)
    }
    navHostController.currentBackStackEntry
        ?.savedStateHandle
        ?.remove<String>("updateDiaryWithTrimmedVideo")

    DisposableEffect(Unit) {
        onDispose {
            isLoading = false
        }
    }

    // 画像/動画選択
    val phonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { mediaUri ->
        mediaUri?.let { url ->
            val mimeType = context.contentResolver.getType(url)

            // TODO 画像が変わったときに古い画像を消さないとゴミデータがどんどん溜まっていく
            // TODO 保存するタイミングが保存時じゃなくて選択時になっているの直さないと
            when {
                mimeType?.startsWith("image") == true -> {
                    val file = viewModel.savePhotoToAppDir(context, url, diary.date)
                    tempDiary = tempDiary.copy(photoPath = file?.toContentUri(context).toString())
                }

                mimeType?.startsWith("video") == true -> {
                    isLoading = true
                    val file = viewModel.saveVideoToAppDir(context, url, diary.date)
                    tempDiary = tempDiary.copy(videoPath = file?.toContentUri(context).toString())
                    navHostController.navigate(
                        AppScreen.VideoEditor(Json.encodeToString(tempDiary)).createRoute()
                    )
                }

                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // ローディングインジケーター
//        if (isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.5f)), // 背景を半透明に
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    modifier = Modifier.width(64.dp),
//                    color = MaterialTheme.colorScheme.secondary,
//                    trackColor = MaterialTheme.colorScheme.surfaceVariant)
//            }
//        }
        DateTitle(date = diary.date)
        MediaContentArea(
            context = context,
            diary = tempDiary,
            onClickAddPhotoOrVideo = {
                phonePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onClickAddLocation = { /* TODO: 位置情報設定画面へ遷移 */ }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiaryContentInput(
            modifier = Modifier,
            diary = tempDiary,
            onContentChange = { tempDiary = tempDiary.copy(content = it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DiarySaveButton(
            onSave = {
                val saveData = tempDiary.copy(uuid = tempDiary.uuid.ifEmpty {
                    UUID.randomUUID().toString() /* 初回保存時 */
                })
                viewModel.saveDiaryToLocal(saveData)
                val json = Json.encodeToString(saveData)
                navHostController.previousBackStackEntry?.savedStateHandle?.set("updateDiary", json)
                navHostController.popBackStack()
            }
        )
    }
}

/**
 * 日付タイトル
 */
@Composable
fun DateTitle(date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(
            text = convertDateFormat(date),
            fontSize = 28.sp
        )
        Text(
            text = getDayOfWeek(date),
            fontSize = 20.sp,
        )
    }
}

/**
 * メディア表示
 */
@Composable
fun MediaContentArea(
    context: Context,
    diary: Diary,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit
) {
    when {
        diary.photoPath != null -> {
            MediaPreView({ PhotoImage(diary.photoPath!!.toUri()) }) { onClickAddLocation() }
        }

        diary.trimmedVideoPath != null -> {
            MediaPreView({
                VideoPlayer(
                    context,
                    diary.trimmedVideoPath!!.toUri()
                )
            }) { onClickAddLocation() }
        }

        else -> {
            NoMediaView(onClickAddPhotoOrVideo)
        }
    }
}

@Composable
fun MediaPreView(
    content: @Composable () -> Unit,
    onClickAddLocation: () -> Unit
) {
    content()
    LocationSetting { onClickAddLocation() }
}

/**
 * メディアがない場合のビュー（追加ボタン）
 * @param onClickAddPhotoOrVideo 写真/動画追加ボタンクリックコールバック
 */
@Composable
fun NoMediaView(onClickAddPhotoOrVideo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Gray)
            .clickable { onClickAddPhotoOrVideo() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "add",
            modifier = Modifier.size(48.dp)
        )
        Text("写真/動画を追加")
    }
}

/**
 * 位置情報設定
 * @param onClickAddLocation 位置情報追加ボタンクリックコールバック
 */
@Composable
fun LocationSetting(onClickAddLocation: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClickAddLocation() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "add",
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
        Text("位置情報を追加")
    }
}

/**
 * 写真プレビュー
 * @param uri 写真URI
 */
@Composable
fun PhotoImage(uri: Uri) {
    AsyncImage(
        model = uri,
        contentDescription = "dairy's photo",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

/**
 * 動画プレビュー
 * @param context Context
 * @param uri 動画URI
 */
@Composable
fun VideoPlayer(context: Context, uri: Uri) {
    val expPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    AndroidView(
        factory = { PlayerView(context).apply { player = expPlayer } },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
    DisposableEffect(Unit) {
        onDispose { expPlayer.release() }
    }
}

/**
 * 内容入力欄
 */
@Composable
fun DiaryContentInput(
    modifier: Modifier,
    diary: Diary,
    onContentChange: (String) -> Unit
) {
    TextField(
        value = diary.content,
        onValueChange = onContentChange,
        label = { Text("内容") },
        modifier = modifier.fillMaxWidth(),
        maxLines = Int.MAX_VALUE,
        singleLine = false
    )
}

/**
 * 保存ボタン
 */
@Composable
fun DiarySaveButton(onSave: () -> Unit) {
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("保存")
    }
}

@Preview(showBackground = true)
@Composable
fun DateDetailViewPreview() {
    DaydydayTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            DateTitle(date = "2022-01-01")
            LocationSetting {}
        }
    }
}