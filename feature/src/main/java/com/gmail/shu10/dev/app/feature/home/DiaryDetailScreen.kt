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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import com.gmail.shu10.dev.app.feature.utils.toContentUri
import java.io.File
import java.util.UUID

/**
 * 詳細ページ
 */
@Composable
fun DiaryDetailScreen(
    selectedDate: String, viewModel: DiaryDetailViewModel = hiltViewModel()
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
                    val file = savePhotoToAppDir(context, url)
                    photoUri = file?.toContentUri(context)
                }

                mimeType?.startsWith("video") == true -> {
                    val file = saveVideoToAppDir(context, url)
                    videoUri = file?.toContentUri(context)
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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DiaryDetailScreen("2025-01-01")
        }
    }
}

/**
 * 写真保存
 */
private fun savePhotoToAppDir(context: Context, uri: Uri): File? {

    val inputStream = context.contentResolver.openInputStream(uri) ?: return null

    val appDir = File(context.filesDir, "images")
    if (!appDir.exists()) appDir.mkdirs()

    val file = File(appDir, "selected_${System.currentTimeMillis()}.jpg")
    inputStream.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}

/**
 * 動画保存
 */
private fun saveVideoToAppDir(context: Context, uri: Uri): File? {

    val inputStream = context.contentResolver.openInputStream(uri) ?: return null

    val appDir = File(context.filesDir, "videos")
    if (!appDir.exists()) appDir.mkdirs()

    val file = File(appDir, "selected_${System.currentTimeMillis()}.mp4")
    inputStream.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}