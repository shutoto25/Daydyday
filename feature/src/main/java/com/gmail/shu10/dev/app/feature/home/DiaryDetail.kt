package com.gmail.shu10.dev.app.feature.home

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.Contacts.Photo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
fun DiaryDetailRoute(
    navHostController: NavHostController,
    navBackStackEntry: NavBackStackEntry,
    viewModel: SharedDiaryViewModel = hiltViewModel(navBackStackEntry),
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    // 状態管理
    var tempDiary by remember { mutableStateOf(viewModel.selectedDiary) }

    DisposableEffect(Unit) {
        onDispose { isLoading = false }
    }

    // メディア選択ロジック（画像・動画の選択後の処理）
    val phonePickerLauncher = rememberPhonePickerLauncher(
        context = context,
        viewModel = viewModel,
        navHostController = navHostController,
        getCurrentDiary = { tempDiary },
        onDiaryUpdated = { updatedDiary -> tempDiary = updatedDiary },
        setLoading = { isLoading = it }
    )

    tempDiary?.let {
//        BackHandler {
//            onBack(it, viewModel)
//        }

        DiaryDetailScreen(
            context = context,
            tempDiary = it,
            onClickAddPhotoOrVideo = {
                phonePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onClickAddLocation = { /* TODO: 位置情報設定画面へ遷移 */ },
            onSave = {
                val saveData = it.copy(uuid = it.uuid.ifEmpty {
                    UUID.randomUUID().toString() /* 初回保存時 */
                })
                viewModel.saveDiaryToLocal(saveData)
                navHostController.popBackStack()
            }
        )
    }
}

private fun onBack(
    diary: Diary,
    viewModel: SharedDiaryViewModel
) {
    val saveData = diary.copy(uuid = diary.uuid.ifEmpty {
        UUID.randomUUID().toString() /* 初回保存時 */
    })
    viewModel.saveDiaryToLocal(saveData)
}

/**
 * PhonePickerLauncherを生成するComposable
 * 内部で選択されたメディアのMIMEタイプに応じた処理をhandleMediaSelection()に委譲
 */
@Composable
private fun rememberPhonePickerLauncher(
    context: Context,
    viewModel: SharedDiaryViewModel,
    navHostController: NavHostController,
    getCurrentDiary: () -> Diary?,
    onDiaryUpdated: (Diary) -> Unit,
    setLoading: (Boolean) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { mediaUri ->
    // 現在の一時日記が null なら早期リターン
    val diary = getCurrentDiary() ?: return@rememberLauncherForActivityResult

    mediaUri?.let { uri ->
        handleMediaSelection(
            context = context,
            uri = uri,
            diary = diary,
            viewModel = viewModel,
            navHostController = navHostController,
            onDiaryUpdated = onDiaryUpdated,
            setLoading = setLoading
        )
    }
}

/**
 * 選択されたメディアのMIMEタイプに応じた処理を実行
 */
private fun handleMediaSelection(
    context: Context,
    uri: Uri,
    diary: Diary,
    viewModel: SharedDiaryViewModel,
    navHostController: NavHostController,
    onDiaryUpdated: (Diary) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    val mimeType = context.contentResolver.getType(uri) ?: return
    when {
        mimeType.startsWith("image") -> {
            val file = viewModel.savePhotoToAppDir(context, uri, diary.date)
            // キャッシュバスティング用にクエリパラメータを追加
            val newPhotoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            onDiaryUpdated(diary.copy(photoPath = newPhotoUri))
        }

        mimeType.startsWith("video") -> {
            setLoading(true)
            val file = viewModel.saveVideoToAppDir(context, uri, diary.date)
            val newVideoUri = file?.toContentUri(context)?.let {
                "$it?ts=${System.currentTimeMillis()}"
            }
            onDiaryUpdated(diary.copy(videoPath = newVideoUri))
            navHostController.navigate(
                AppScreen.VideoEditor(
                    Json.encodeToString(diary.copy(videoPath = newVideoUri))
                ).createRoute()
            )
        }

        else -> {
            // 必要なら他のメディアタイプへの処理を追加
        }
    }
}

@Composable
private fun DiaryDetailScreen(
    context: Context,
    tempDiary: Diary,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
    onSave: () -> Unit,
) {
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
        DateTitle(date = tempDiary.date)
        MediaContentArea(
            context = context,
            diary = tempDiary,
            onClickAddPhotoOrVideo = { onClickAddPhotoOrVideo() },
            onClickAddLocation = { onClickAddLocation() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiaryContentInput(
            modifier = Modifier,
            diary = tempDiary,
            onContentChange = { /* あとで */ }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiarySaveButton(onSave = { onSave() })
    }

}

/**
 * 日付タイトル
 */
@Composable
private fun DateTitle(date: String) {
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
private fun MediaContentArea(
    context: Context,
    diary: Diary,
    onClickAddPhotoOrVideo: () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    when {
        diary.photoPath != null -> {
            MediaPreView({
                PhotoImage(
                    diary.photoPath!!.toUri(),
                    onClickAddPhotoOrVideo
                )
            }) { onClickAddLocation() }
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
private fun MediaPreView(
    content: @Composable () -> Unit,
    onClickAddLocation: () -> Unit,
) {
    content()
    LocationSetting { onClickAddLocation() }
}

/**
 * メディアがない場合のビュー（追加ボタン）
 * @param onClickAddPhotoOrVideo 写真/動画追加ボタンクリックコールバック
 */
@Composable
private fun NoMediaView(onClickAddPhotoOrVideo: () -> Unit) {
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
private fun LocationSetting(onClickAddLocation: () -> Unit) {
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
private fun PhotoImage(uri: Uri, onRefreshClick: () -> Unit) {
    Box {
        AsyncImage(
            model = uri,
            contentDescription = "dairy's photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "change",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(32.dp)
                .clickable { onRefreshClick() }
        )
    }
}

/**
 * 動画プレビュー
 * @param context Context
 * @param uri 動画URI
 */
@Composable
private fun VideoPlayer(context: Context, uri: Uri) {
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
private fun DiaryContentInput(
    modifier: Modifier,
    diary: Diary,
    onContentChange: (String) -> Unit,
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
private fun DiarySaveButton(onSave: () -> Unit) {
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("保存")
    }
}

@Preview(showBackground = true)
@Composable
private fun DateDetailViewPreview() {
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