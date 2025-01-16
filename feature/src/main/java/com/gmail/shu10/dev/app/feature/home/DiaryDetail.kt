package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gmail.shu10.dev.app.core.utils.convertDateFormat
import com.gmail.shu10.dev.app.core.utils.getDayOfWeek
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import java.util.UUID

/**
 * 日記詳細画面
 */
@Composable
fun DiaryDetailScreen(
    navHostController: NavHostController,
    diary: Diary,
    viewModel: DiaryDetailViewModel = hiltViewModel()
) {
    // FlowをcollectAsStateで監視
//    val selectedDate by viewModel.getDiaryByDate(diary.date).collectAsState(initial = null)
    // 状態管理
    val title by remember { mutableStateOf(diary.title) }
    var content by remember { mutableStateOf(diary.content) }
    var photoUri by remember { mutableStateOf(diary.photoPath?.toUri()) }
    val videoUri by remember { mutableStateOf(diary.videoPath?.toUri()) }
    // 状態監視
//    LaunchedEffect(selectedDate) {
//        title = selectedDate?.title ?: ""
//        content = selectedDate?.content ?: ""
//        photoUri = selectedDate?.photoPath?.toUri()
//        videoUri = selectedDate?.videoPath?.toUri()
//    }

    val context = LocalContext.current
    // 画像/動画選択
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
                    navHostController.navigate("videoEditor")
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
            .padding(8.dp)
    ) {
        DateTitle(date = diary.date)
        MediaContentArea(
            context = context,
            videoUri = videoUri,
            photoUri = photoUri,
            onClickAddPhotoOrVideo = {
                phonePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onClickAddLocation = { /* TODO: 位置情報設定画面へ遷移 */ }
        )
//        DiaryTitleInput(
//            title = title,
//            onTitleChange = { title = it }
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        MediaPreview(uri = photoUri)
//        Spacer(modifier = Modifier.height(16.dp))
//        VideoPlayer(context = context, uri = videoUri)
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
                    id = diary.id,
                    uuid = diary.uuid.ifEmpty { UUID.randomUUID().toString() },
                    date = diary.date,
                    title = title,
                    content = content,
                    photoPath = photoUri?.toString(),
                    videoPath = videoUri?.toString(),
                    location = diary.location,
                    isSynced = diary.isSynced
                )
                viewModel.saveDiary(saveData)
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
 * メディア表示
 */
@Composable
fun MediaContentArea(
    context: Context,
    photoUri: Uri?,
    videoUri: Uri?,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit
) {
    when {
        photoUri != null -> {
            MediaPreView({ PhotoImage(photoUri) }) { onClickAddLocation() }
        }

        videoUri != null -> {
            MediaPreView({ VideoPlayer(context, videoUri) }) { onClickAddLocation() }
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