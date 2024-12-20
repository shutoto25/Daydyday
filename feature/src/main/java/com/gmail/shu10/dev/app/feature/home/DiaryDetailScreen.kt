package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import java.util.UUID

/**
 * 日記詳細画面
 */
@Composable
fun DiaryDetailScreen(
    navHostController: NavHostController,
    selectedDate: String,
    viewModel: DiaryDetailViewModel = hiltViewModel()
) {
    // FlowをcollectAsStateで監視
    val diary by viewModel.getDiaryByDate(selectedDate).collectAsState(initial = null)
    // 状態管理
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    // 状態監視
    LaunchedEffect(diary) {
        title = diary?.title ?: ""
        content = diary?.content ?: ""
        photoUri = diary?.photoPath?.toUri()
        videoUri = diary?.videoPath?.toUri()
    }

    val context = LocalContext.current
    val phonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { mediaUri ->
        mediaUri?.let { url ->
            val mimeType = context.contentResolver.getType(url)

            // TODO 画像が変わったときに古い画像を消さないとゴミデータがどんどん溜まっていく
            when {
                mimeType?.startsWith("image") == true -> {
                    val file = viewModel.savePhotoToAppDir(context, url)
                    photoUri = file?.toContentUri(context)
                }

                mimeType?.startsWith("video") == true -> {
                    navHostController.currentBackStackEntry?.savedStateHandle?.set(
                        "selectedVideoUri", url.toString()
                    )
                    navHostController.navigate("videoEdit")
//                    val file = saveVideoToAppDir(context, url)
//                    videoUri = file?.toContentUri(context)
                }

                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Selected Date: $selectedDate",
        )
        DiaryTitleInput(
            title = title,
            onTitleChange = { title = it }
        )
        DiaryActionButtons(
            onClickAddPhotoOrVideo = {
                phonePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onClickAddLocation = { /* TODO: 位置情報を追加 */ }
        )
        Spacer(modifier = Modifier.height(16.dp))
        MediaPreview(uri = photoUri)
        Spacer(modifier = Modifier.height(16.dp))
        VideoPlayer(context = context, uri = videoUri)
        Spacer(modifier = Modifier.height(16.dp))
        DiaryContentInput(
            modifier = Modifier,
            content = content,
            onContentChange = { content = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DiarySaveButton(
            onSave = {
                val saveData = Diary(
                    id = diary?.id,
                    uuid = diary?.uuid ?: UUID.randomUUID().toString(),
                    date = selectedDate,
                    title = title,
                    content = content,
                    photoPath = photoUri.toString(),
                    videoPath = videoUri.toString(),
                    location = null,
                    isSynced = false
                )
                viewModel.saveDiary(saveData)
            }
        )
    }
}

/**
 * タイトル入力欄
 */
@Composable
fun DiaryTitleInput(title: String, onTitleChange: (String) -> Unit) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("タイトル") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = Int.MAX_VALUE,
        singleLine = true
    )
}

/**
 * 追加ボタン
 */
@Composable
fun DiaryActionButtons(
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(4.dp),
            onClick = onClickAddPhotoOrVideo
        ) {
            Text("写真/動画を追加")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(4.dp),
            onClick = onClickAddLocation
        ) {
            Text("位置情報を追加")
        }
    }
}

/**
 * メディアプレビュー
 */
@Composable
fun MediaPreview(uri: Uri?) {
    uri?.let {
        AsyncImage(
            model = uri,
            contentDescription = "dairy's photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

/**
 * 動画プレビュー
 */
@Composable
fun VideoPlayer(context: Context, uri: Uri?) {
    uri?.let {
        val expPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
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
}

/**
 * 内容入力欄
 */
@Composable
fun DiaryContentInput(
    modifier: Modifier,
    content: String,
    onContentChange: (String) -> Unit
) {
    TextField(
        value = content,
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

//@Preview(showBackground = true)
//@Composable
//fun DateDetailViewPreview() {
//    DaydydayTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = MaterialTheme.colorScheme.background
//        ) {
//            DiaryDetailScreen("2025-01-01")
//        }
//    }
//}